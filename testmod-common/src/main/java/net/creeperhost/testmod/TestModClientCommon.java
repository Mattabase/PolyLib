package net.creeperhost.testmod;

import net.creeperhost.polylib.chat.ChatChannel;
import net.creeperhost.polylib.chat.ChatMember;
import net.creeperhost.polylib.chat.ChatRouter;
import net.creeperhost.polylib.chat.RichChatMessage;
import net.creeperhost.polylib.chat.client.ChatNotifications;
import net.creeperhost.polylib.chat.client.FloatingChatWindow;
import net.creeperhost.polylib.chat.client.notification.NotificationEntry;
import net.creeperhost.polylib.chat.client.notification.NotificationSeverity;
import net.creeperhost.polylib.chat.client.notification.PolyNotifications;
import net.creeperhost.polylib.chat.client.tab.ChatTabRegistry;
import net.creeperhost.polylib.client.modulargui.ModularGuiInjector;
import net.creeperhost.polylib.client.modulargui.ModularGuiScreen;
import net.creeperhost.polylib.client.modulargui.lib.geometry.Constraint;
import net.creeperhost.polylib.client.modulargui.lib.geometry.GeoParam;
import net.creeperhost.polylib.client.modulargui.nodegraph.NodeTypeRegistry;
import net.creeperhost.polylib.client.modulargui.nodegraph.graph.NodeGraph;
import net.creeperhost.polylib.event.events.client.PolyInputEvents;
import net.creeperhost.testmod.init.TestClientEvents;
import net.creeperhost.testmod.init.TestDebugEntries;
import net.creeperhost.testmod.init.TestScreens;
import net.creeperhost.testmod.nodegraph.TestAnyRelayNodeType;
import net.creeperhost.testmod.nodegraph.TestFluidBufferNodeType;
import net.creeperhost.testmod.nodegraph.TestItemPassthroughNodeType;
import net.creeperhost.testmod.nodegraph.TestSignalSinkNodeType;
import net.creeperhost.testmod.nodegraph.TestSignalSourceNodeType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;
import java.util.UUID;
import static net.creeperhost.testmod.TestModCommon.LOGGER;

public class TestModClientCommon
{
    // --- Channel IDs ---
    static final Identifier CH_GENERAL  = Identifier.fromNamespaceAndPath("testmod", "general");
    static final Identifier CH_GUILD    = Identifier.fromNamespaceAndPath("testmod", "guild");
    static final Identifier CH_SYSTEM   = Identifier.fromNamespaceAndPath("testmod", "system");
    static final Identifier CH_TRADE    = Identifier.fromNamespaceAndPath("testmod", "trade");

    public static void init()
    {
        TestClientEvents.init();
        TestScreens.init();
        TestDebugEntries.init();
        ModularGuiInjector.registerInjection(e -> e instanceof TitleScreen, e -> new MainMenuGuiInjection());

        // -----------------------------------------------------------------------
        // 1. Register chat channels
        // -----------------------------------------------------------------------

        // General — TOP tab, player can submit messages (loopback echo)
        ChatChannel general = new ChatChannel(CH_GENERAL, Component.literal("General"), true);
        general.setSubmitHandler(text -> {
            Minecraft mc2 = Minecraft.getInstance();
            String sender = mc2.player != null ? mc2.player.getScoreboardName() : "Player";
            general.addMessage(RichChatMessage.create(Component.literal(text), Component.literal(sender)));
        });
        general.addMember(new ChatMember(UUID.randomUUID(), Component.literal("Steve"), null, true));
        general.addMember(new ChatMember(UUID.randomUUID(), Component.literal("Alex"), null, false));
        general.addMessage(RichChatMessage.create(Component.literal("Welcome to the General channel!"), Component.literal("Server")));
        general.addMessage(RichChatMessage.create(Component.literal("Type something in chat to try submitting a message."), Component.literal("Server")));
        ChatRouter.getInstance().registerChannel(general);
        ChatTabRegistry.get().registerTop(general);

        // Guild — TOP tab, second channel for switching/pulse test
        ChatChannel guild = new ChatChannel(CH_GUILD, Component.literal("Guild"), false);
        guild.setSubmitHandler(text -> {
            Minecraft mc2 = Minecraft.getInstance();
            String sender = mc2.player != null ? mc2.player.getScoreboardName() : "Player";
            guild.addMessage(RichChatMessage.create(Component.literal(text), Component.literal(sender)));
        });
        guild.addMessage(RichChatMessage.create(Component.literal("Guild HQ reporting in."), Component.literal("Guildmaster")));
        guild.addMessage(RichChatMessage.create(Component.literal("Use KP_6 to trigger a mention pulse on this channel."), Component.literal("Server")));
        ChatRouter.getInstance().registerChannel(guild);
        ChatTabRegistry.get().registerTop(guild);

        // System — SIDE_LEFT tab — read-only server alerts
        ChatChannel system = new ChatChannel(CH_SYSTEM, Component.literal("Sys"), false);
        system.addMessage(RichChatMessage.create(Component.literal("[INFO] Server started successfully."), Component.literal("System")));
        system.addMessage(RichChatMessage.create(Component.literal("[WARN] TPS dropped to 15 for 2 seconds."), Component.literal("System")));
        system.addMessage(RichChatMessage.create(Component.literal("[INFO] 3 players online."), Component.literal("System")));
        ChatRouter.getInstance().registerChannel(system);
        ChatTabRegistry.get().registerSideLeft(system);

        // Trade — SIDE_RIGHT tab — economy/trade chat
        ChatChannel trade = new ChatChannel(CH_TRADE, Component.literal("Trade"), false);
        trade.setSubmitHandler(text -> {
            Minecraft mc2 = Minecraft.getInstance();
            String sender = mc2.player != null ? mc2.player.getScoreboardName() : "Player";
            trade.addMessage(RichChatMessage.create(Component.literal(text), Component.literal(sender)));
        });
        trade.addMessage(RichChatMessage.create(Component.literal("WTS 64x Diamond — 50g each, /msg Steve"), Component.literal("Steve")));
        trade.addMessage(RichChatMessage.create(Component.literal("WTB Iron Ingots in bulk — DM me"), Component.literal("Alex")));
        ChatRouter.getInstance().registerChannel(trade);
        ChatTabRegistry.get().registerSideRight(trade);

        // -----------------------------------------------------------------------
        // 2. Register shared notifications window as a TOP tab (Plan D)
        // -----------------------------------------------------------------------
        PolyNotifications.init(); // ensure the shared window exists
        ChatTabRegistry.get().registerNotificationTop(
            PolyNotifications.SHARED_WINDOW_ID,
            Component.literal("Notifs")
        );

        // -----------------------------------------------------------------------
        // 3. Mention pulse: register TRIGGER_ANY_MESSAGE on the System channel
        //    so every system message pulses the side-left tab
        // -----------------------------------------------------------------------
        ChatNotifications.registerMentionTrigger(CH_SYSTEM, ChatNotifications.TRIGGER_ANY_MESSAGE);

        // -----------------------------------------------------------------------
        // 4. Keybinds (all fire on key-press only, outside any open screen)
        // -----------------------------------------------------------------------
        PolyInputEvents.INPUT_KEY.register((key, scanCode, action, modifiers) ->
        {
            if (action != 1) return; // press only
            Minecraft mc = Minecraft.getInstance();
            if (mc.screen != null) return;

            // ------------------------------------------------------------------
            // KP_5 — open all channels as floating windows (Plan B test)
            // ------------------------------------------------------------------
            if (key == GLFW.GLFW_KEY_KP_5)
            {
                LOGGER.info("[TestMod] KP_5: opening FloatingChatWindow for all channels");
                mc.setScreen(new ModularGuiScreen(gui ->
                {
                    gui.initFullscreenGui();
                    int offset = 0;
                    for (ChatChannel ch : ChatRouter.getInstance().getActiveChannels())
                    {
                        FloatingChatWindow win = new FloatingChatWindow(gui.getRoot(), ch);
                        win.constrain(GeoParam.LEFT,   Constraint.literal(40 + offset * 25))
                           .constrain(GeoParam.TOP,    Constraint.literal(40 + offset * 25))
                           .constrain(GeoParam.WIDTH,  Constraint.literal(280))
                           .constrain(GeoParam.HEIGHT, Constraint.literal(200));
                        offset++;
                    }
                }));
            }

            // ------------------------------------------------------------------
            // KP_6 — simulate a mention in Guild to trigger pulse (Plan C test)
            // ------------------------------------------------------------------
            if (key == GLFW.GLFW_KEY_KP_6)
            {
                String playerName = mc.player != null ? mc.player.getScoreboardName() : "Player";
                LOGGER.info("[TestMod] KP_6: simulating mention of '{}' in Guild", playerName);
                guild.addMessage(RichChatMessage.create(
                    Component.literal("Hey " + playerName + "! You got a mention."),
                    Component.literal("Alex")
                ));
                // Also pulse System channel with a new alert
                system.addMessage(RichChatMessage.create(
                    Component.literal("[ALERT] Player " + playerName + " triggered KP_6 test"),
                    Component.literal("System")
                ));
            }

            // ------------------------------------------------------------------
            // KP_7 — send test notifications (Plan D test)
            // ------------------------------------------------------------------
            if (key == GLFW.GLFW_KEY_KP_7)
            {
                Identifier src = Identifier.fromNamespaceAndPath("testmod", "test");
                PolyNotifications.send(NotificationEntry.create(
                    src,
                    Component.literal("Server Info"),
                    Component.literal("3 players are online right now."),
                    NotificationSeverity.INFO
                ));
                PolyNotifications.send(NotificationEntry.create(
                    src,
                    Component.literal("Low Resources"),
                    Component.literal("Server TPS is below 18. Expect lag."),
                    NotificationSeverity.WARNING
                ));
                PolyNotifications.send(NotificationEntry.create(
                    src,
                    Component.literal("PVP Alert"),
                    Component.literal("A hostile player is nearby!"),
                    NotificationSeverity.ALERT
                ));
                LOGGER.info("[TestMod] KP_7: sent INFO + WARNING + ALERT notifications (check 'Notifs' tab)");
            }

            // ------------------------------------------------------------------
            // KP_8 — flood all channels with rapid messages (stress test)
            // ------------------------------------------------------------------
            if (key == GLFW.GLFW_KEY_KP_8)
            {
                LOGGER.info("[TestMod] KP_8: flooding all channels with 10 messages each");
                for (int i = 1; i <= 10; i++)
                {
                    general.addMessage(RichChatMessage.create(Component.literal("Flood message #" + i), Component.literal("Bot")));
                    guild.addMessage(RichChatMessage.create(Component.literal("Guild flood #" + i), Component.literal("GuildBot")));
                    system.addMessage(RichChatMessage.create(Component.literal("[INFO] System message #" + i), Component.literal("System")));
                    trade.addMessage(RichChatMessage.create(Component.literal("WTS Item #" + i + " — DM me"), Component.literal("Trader")));
                }
            }

            // ------------------------------------------------------------------
            // KP_9 — send one of each notification severity to verify badges
            // ------------------------------------------------------------------
            if (key == GLFW.GLFW_KEY_KP_9)
            {
                Identifier src = Identifier.fromNamespaceAndPath("testmod", "badge_test");
                for (NotificationSeverity sev : NotificationSeverity.values())
                {
                    PolyNotifications.send(NotificationEntry.create(
                        src,
                        Component.literal(sev.name() + " Badge Test"),
                        Component.literal("Testing unread badge counter for " + sev.name()),
                        sev
                    ));
                }
                LOGGER.info("[TestMod] KP_9: sent badge test notifications (3 unread — check tab badge count)");
            }
        });

        LOGGER.info("[TestMod] Chat system test channels registered:");
        LOGGER.info("  TOP  tabs : General, Guild, Notifs");
        LOGGER.info("  LEFT tab  : Sys  (side-left bar)");
        LOGGER.info("  RIGHT tab : Trade (side-right bar)");
        LOGGER.info("  KP_5 = open floating windows | KP_6 = trigger mention pulse");
        LOGGER.info("  KP_7 = send 3 notifications  | KP_8 = flood messages | KP_9 = badge test");
        LOGGER.info("  KP_4 = open node graph demo   | KP_0 = run graph API verification");

        // Register test node types for the node graph demo (covers all 4 port data types)
        NodeTypeRegistry.register(TestSignalSourceNodeType.INSTANCE);   // SIGNAL out
        NodeTypeRegistry.register(TestSignalSinkNodeType.INSTANCE);     // SIGNAL in
        NodeTypeRegistry.register(TestItemPassthroughNodeType.INSTANCE);// ITEMS in + out
        NodeTypeRegistry.register(TestFluidBufferNodeType.INSTANCE);    // FLUID in + out
        NodeTypeRegistry.register(TestAnyRelayNodeType.INSTANCE);       // ANY in + out

        // -----------------------------------------------------------------------
        // 5. KP_0 — in-game NodeGraph API verification (graph data model unit tests)
        //    Exercises: addNode, addConnection, hasCycle, removeConnection,
        //               removeNode, type-compatibility rejection, NBT round-trip.
        // -----------------------------------------------------------------------
        PolyInputEvents.INPUT_KEY.register((key, scanCode, action, modifiers) ->
        {
            if (action != 1) return; // press only
            Minecraft mc = Minecraft.getInstance();
            if (mc.screen != null) return;
            if (key != GLFW.GLFW_KEY_KP_0) return;

            LOGGER.info("[TestMod] KP_0: running NodeGraph API verification...");
            int pass = 0, fail = 0;

            // 1. addNode
            NodeGraph g = new NodeGraph();
            UUID srcId  = UUID.randomUUID();
            UUID sinkId = UUID.randomUUID();
            UUID relayId = UUID.randomUUID();
            g.addNode(new net.creeperhost.polylib.client.modulargui.nodegraph.graph.NodeDef(srcId,  TestSignalSourceNodeType.ID,   0, 0, null));
            g.addNode(new net.creeperhost.polylib.client.modulargui.nodegraph.graph.NodeDef(sinkId, TestSignalSinkNodeType.ID,   200, 0, null));
            g.addNode(new net.creeperhost.polylib.client.modulargui.nodegraph.graph.NodeDef(relayId,TestAnyRelayNodeType.ID,     400, 0, null));
            if (g.nodes().size() == 3) { LOGGER.info("  [PASS] addNode x3"); pass++; }
            else                       { LOGGER.error("  [FAIL] addNode: expected 3, got {}", g.nodes().size()); fail++; }

            // 2. addConnection (valid: SIGNAL out → SIGNAL in)
            UUID connId = UUID.randomUUID();
            boolean added = g.addConnection(new net.creeperhost.polylib.client.modulargui.nodegraph.graph.ConnectionDef(connId, srcId, 0, sinkId, 0));
            if (added) { LOGGER.info("  [PASS] addConnection SIGNAL→SIGNAL"); pass++; }
            else       { LOGGER.error("  [FAIL] addConnection SIGNAL→SIGNAL rejected unexpectedly"); fail++; }

            // 3. Duplicate input-port rejection (same toNode+toPort must be rejected)
            boolean dup = g.addConnection(new net.creeperhost.polylib.client.modulargui.nodegraph.graph.ConnectionDef(UUID.randomUUID(), srcId, 0, sinkId, 0));
            if (!dup) { LOGGER.info("  [PASS] duplicate input-port connection rejected"); pass++; }
            else      { LOGGER.error("  [FAIL] duplicate input-port connection was accepted"); fail++; }

            // 4. ANY compatibility: SIGNAL out → ANY in (relay)
            boolean anyOk = g.addConnection(new net.creeperhost.polylib.client.modulargui.nodegraph.graph.ConnectionDef(UUID.randomUUID(), srcId, 0, relayId, 0));
            if (anyOk) { LOGGER.info("  [PASS] SIGNAL→ANY compatibility"); pass++; }
            else       { LOGGER.error("  [FAIL] SIGNAL→ANY rejected"); fail++; }

            // 5. Cycle detection (src→sink→src would be a cycle; test self-loop)
            boolean selfLoop = g.addConnection(new net.creeperhost.polylib.client.modulargui.nodegraph.graph.ConnectionDef(UUID.randomUUID(), srcId, 0, srcId, 0));
            if (!selfLoop) { LOGGER.info("  [PASS] self-loop cycle rejected"); pass++; }
            else           { LOGGER.error("  [FAIL] self-loop was accepted"); fail++; }

            // 6. removeConnection
            boolean removed = g.removeConnection(connId);
            if (removed && g.connections().size() == 1) { LOGGER.info("  [PASS] removeConnection"); pass++; }
            else { LOGGER.error("  [FAIL] removeConnection: removed={} connections={}", removed, g.connections().size()); fail++; }

            // 7. removeNode (also removes its connections)
            g.removeNode(sinkId);
            if (!g.nodes().containsKey(sinkId)) { LOGGER.info("  [PASS] removeNode"); pass++; }
            else { LOGGER.error("  [FAIL] removeNode: node still present"); fail++; }

            // 8. NBT round-trip
            NodeGraph g2 = new NodeGraph();
            UUID a = UUID.randomUUID(), b = UUID.randomUUID();
            g2.addNode(new net.creeperhost.polylib.client.modulargui.nodegraph.graph.NodeDef(a, TestSignalSourceNodeType.ID,   0, 0, null));
            g2.addNode(new net.creeperhost.polylib.client.modulargui.nodegraph.graph.NodeDef(b, TestSignalSinkNodeType.ID,   200, 0, null));
            g2.addConnection(new net.creeperhost.polylib.client.modulargui.nodegraph.graph.ConnectionDef(UUID.randomUUID(), a, 0, b, 0));
            NodeGraph g3 = NodeGraph.fromNbt(g2.toNbt());
            if (g3.nodes().size() == 2 && g3.connections().size() == 1) { LOGGER.info("  [PASS] NBT round-trip"); pass++; }
            else { LOGGER.error("  [FAIL] NBT round-trip: nodes={} conns={}", g3.nodes().size(), g3.connections().size()); fail++; }

            LOGGER.info("[TestMod] KP_0 complete: {} passed, {} failed", pass, fail);
        });
    }
}

