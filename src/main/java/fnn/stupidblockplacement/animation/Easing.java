package fnn.stupidblockplacement.animation;

import net.minecraft.network.chat.Component;

public enum Easing {
    CONSTANT {
        @Override
        public float apply(float t) {
            return 1.0F;
        }

        @Override
        public String getTranslation() {
            return "stupid-block-animations.easings.constant";
        }
    },
    LINEAR {
        @Override
        public float apply(float t) {
            return 1.0F - t;
        }

        @Override
        public String getTranslation() {
            return "stupid-block-animations.easings.linear";
        }
    },
    EASE_OUT {
        @Override
        public float apply(float t) {
            float inv = 1.0F - t;
            return inv * inv;
        }

        @Override
        public String getTranslation() {
            return "stupid-block-animations.easings.ease_out";
        }
    },
    EASE_IN_OUT {
        @Override
        public float apply(float t) {
            if (t < 0.5F) {
                return 1.0F - (2.0F * t * t);
            }
            float inv = 1.0F - t;
            return 2.0F * inv * inv;
        }

        @Override
        public String getTranslation() {
            return "stupid-block-animations.easings.ease_in_out";
        }
    };

    public abstract float apply(float t);
    public abstract String getTranslation();

    public Component asText() {
        return Component.translatable(getTranslation());
    }
}
