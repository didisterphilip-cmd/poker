# Keep the poker engine and AI (pure logic, reflection-free) fully intact.
-keep class com.homepoker.engine.** { *; }
-keep class com.homepoker.ai.** { *; }

# Compose handles its own keep rules via the AGP-bundled configuration.
