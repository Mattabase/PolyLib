package net.creeperhost.polylib.client.modulargui.builder;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Watches a single JSON layout file for changes and fires a callback when it is modified.
 *
 * <p>Uses a {@link WatchService} on the parent directory. Call {@link #tick()} from the
 * client tick event (e.g. {@code PolyClientTickEvents.CLIENT_TICK_END}) to poll for changes.
 *
 * <p>Only one callback is fired per physical file-modification event, even if the file is
 * written multiple times rapidly within a single tick window.
 *
 * <p>Call {@link #close()} when the watcher is no longer needed to release OS resources.
 */
public class LayoutWatcher implements AutoCloseable {

    private static final Logger LOGGER = LogManager.getLogger(LayoutWatcher.class);

    /** All active watchers — ticked globally. */
    private static final List<LayoutWatcher> ACTIVE = new ArrayList<>();

    private final Path targetFile;
    private final Runnable onChange;
    private WatchService watchService;
    private WatchKey watchKey;
    private boolean closed = false;

    public LayoutWatcher(Path targetFile, Runnable onChange) {
        this.targetFile = targetFile.toAbsolutePath().normalize();
        this.onChange = onChange;
        try {
            this.watchService = FileSystems.getDefault().newWatchService();
            Path parent = this.targetFile.getParent();
            if (parent != null) {
                this.watchKey = parent.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY);
            }
            synchronized (ACTIVE) { ACTIVE.add(this); }
        } catch (IOException e) {
            LOGGER.error("LayoutWatcher: failed to watch '{}'", targetFile, e);
        }
    }

    /**
     * Poll for pending file-change events.  Must be called regularly (e.g. every client tick).
     */
    public void tick() {
        if (closed || watchService == null) return;
        WatchKey key = watchService.poll();
        if (key == null) return;
        boolean modified = false;
        for (WatchEvent<?> event : key.pollEvents()) {
            if (event.kind() == StandardWatchEventKinds.OVERFLOW) continue;
            @SuppressWarnings("unchecked")
            WatchEvent<Path> pathEvent = (WatchEvent<Path>) event;
            Path changed = targetFile.getParent().resolve(pathEvent.context()).toAbsolutePath().normalize();
            if (targetFile.equals(changed)) {
                modified = true;
            }
        }
        key.reset();
        if (modified) {
            try {
                onChange.run();
            } catch (Exception e) {
                LOGGER.error("LayoutWatcher: onChange callback threw for '{}'", targetFile, e);
            }
        }
    }

    @Override
    public void close() {
        closed = true;
        synchronized (ACTIVE) { ACTIVE.remove(this); }
        try {
            if (watchKey != null) watchKey.cancel();
            if (watchService != null) watchService.close();
        } catch (IOException ignored) {}
    }

    // ── Global tick ───────────────────────────────────────────────────────────

    /**
     * Called from {@code InternalEventListenerClient} each client tick to poll all active watchers.
     */
    public static void tickAll() {
        List<LayoutWatcher> snapshot;
        synchronized (ACTIVE) { snapshot = new ArrayList<>(ACTIVE); }
        for (LayoutWatcher w : snapshot) w.tick();
    }
}
