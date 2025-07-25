package net.minecraft.server.network;

import java.util.function.Consumer;
import net.minecraft.network.protocol.Packet;

public interface ConfigurationTask {
    default void start(net.minecraftforge.network.config.ConfigurationTaskContext ctx) {
        start(ctx::send);
    }

    void start(Consumer<Packet<?>> p_299398_);

    ConfigurationTask.Type type();

    public record Type(String id) {
        @Override
        public String toString() {
            return this.id;
        }
    }
}
