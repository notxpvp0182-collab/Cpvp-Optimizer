package com.ahmed.cpvpoptimizer.common;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public abstract class ModuleSetting<T> {
    private final String key;
    private final String displayName;
    private final String description;
    private final T defaultValue;
    private T value;
    private final Consumer<T> onChange;

    protected ModuleSetting(String key, String displayName, String description, T defaultValue, Consumer<T> onChange) {
        this.key = Objects.requireNonNull(key);
        this.displayName = Objects.requireNonNull(displayName);
        this.description = Objects.requireNonNull(description);
        this.defaultValue = Objects.requireNonNull(defaultValue);
        this.value = defaultValue;
        this.onChange = onChange != null ? onChange : v -> {};
    }

    public String getKey() { return key; }
    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
    public T getDefaultValue() { return defaultValue; }
    public T getValue() { return value; }

    public void setValue(T newValue) {
        T validated = validate(newValue);
        if (!Objects.equals(this.value, validated)) {
            this.value = validated;
            this.onChange.accept(this.value);
        }
    }

    public void reset() {
        setValue(defaultValue);
    }

    protected abstract T validate(T candidate);
    public abstract String formatValue();

    public static final class BooleanSetting extends ModuleSetting<Boolean> {
        public BooleanSetting(String key, String displayName, String description, boolean defaultValue, Consumer<Boolean> onChange) {
            super(key, displayName, description, defaultValue, onChange);
        }

        @Override
        protected Boolean validate(Boolean candidate) {
            return candidate != null ? candidate : getDefaultValue();
        }

        public void toggle() {
            setValue(!getValue());
        }

        @Override
        public String formatValue() {
            return getValue() ? "ON" : "OFF";
        }
    }

    public static final class IntSliderSetting extends ModuleSetting<Integer> {
        private final int min;
        private final int max;
        private final String unit;

        public IntSliderSetting(String key, String displayName, String description, int defaultValue, int min, int max, String unit, Consumer<Integer> onChange) {
            super(key, displayName, description, defaultValue, onChange);
            this.min = min;
            this.max = max;
            this.unit = unit != null ? unit : "";
        }

        public int getMin() { return min; }
        public int getMax() { return max; }

        @Override
        protected Integer validate(Integer candidate) {
            if (candidate == null) return getDefaultValue();
            return Math.max(min, Math.min(max, candidate));
        }

        @Override
        public String formatValue() {
            return getValue() + (unit.isEmpty() ? "" : " " + unit);
        }
    }

    public static final class CycleSetting extends ModuleSetting<String> {
        private final List<String> options;

        public CycleSetting(String key, String displayName, String description, String defaultValue, List<String> options, Consumer<String> onChange) {
            super(key, displayName, description, defaultValue, onChange);
            this.options = List.copyOf(options);
        }

        public List<String> getOptions() { return options; }

        public void cycleNext() {
            int idx = options.indexOf(getValue());
            int next = (idx + 1) % options.size();
            setValue(options.get(next));
        }

        @Override
        protected String validate(String candidate) {
            return (candidate != null && options.contains(candidate)) ? candidate : getDefaultValue();
        }

        @Override
        public String formatValue() {
            return getValue();
        }
    }

    public static final class TextSetting extends ModuleSetting<String> {
        private final int maxLength;

        public TextSetting(String key, String displayName, String description, String defaultValue, int maxLength, Consumer<String> onChange) {
            super(key, displayName, description, defaultValue, onChange);
            this.maxLength = Math.max(4, maxLength);
        }

        public int getMaxLength() { return maxLength; }

        @Override
        protected String validate(String candidate) {
            if (candidate == null) return getDefaultValue();
            String trimmed = candidate.stripLeading();
            return trimmed.length() > maxLength ? trimmed.substring(0, maxLength) : trimmed;
        }

        @Override
        public String formatValue() {
            return getValue();
        }
    }
}
