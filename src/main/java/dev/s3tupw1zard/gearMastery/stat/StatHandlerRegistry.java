package dev.s3tupw1zard.gearMastery.stat;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.Collection;

public final class StatHandlerRegistry {
    private final Map<StatType, StatHandler> handlers = new EnumMap<>(StatType.class);
    public void register(final StatHandler handler) { handlers.put(handler.type(), handler); }
    public Optional<StatHandler> find(final StatType type) { return Optional.ofNullable(handlers.get(type)); }
    public Collection<StatHandler> handlers() { return handlers.values(); }
}
