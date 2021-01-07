package com.orlinskas.videofacefinder.data.enums


object Settings {

    enum class Fps(val float: Float) {
        MAX(2.0F),
        DEFAULT(1.0F),
        ALMOST_MIN(0.5F),
        MIN(0.1F);

        companion object {

            fun getSliderValue(fps: Fps): Float {
                return when(fps) {
                    MIN -> 1F
                    ALMOST_MIN -> 2F
                    DEFAULT -> 3F
                    MAX -> 4F
                }
            }

            fun fromValue(float: Float): Fps {
                return when(float) {
                    1F -> MIN
                    2F -> ALMOST_MIN
                    3F -> DEFAULT
                    4F -> MAX
                    else -> DEFAULT
                }
            }
        }
    }

    enum class Compress(val int: Int, val readable: String) {
        MAX(31, ">90%"),
        MEDIUM(10, "50%"),
        DEFAULT(3, "10%"),
        OFF(1, "Without compression");

        companion object {

            fun getSliderValue(compress: Compress): Float {
                return when(compress) {
                    OFF -> 1F
                    DEFAULT -> 2F
                    MEDIUM -> 3F
                    MAX -> 4F
                }
            }

            fun fromValue(float: Float): Compress {
                return when(float) {
                    1F -> OFF
                    2F -> DEFAULT
                    3F -> MEDIUM
                    4F -> MAX
                    else -> DEFAULT
                }
            }
        }
    }

    enum class Scale(val int: Int) {
        FIVE(5),
        FOUR(4),
        THREE(3),
        TWO(2),
        DEFAULT(1);

        companion object {

            fun getSliderValue(scale: Scale): Float {
                return scale.int.toFloat()
            }

            fun fromValue(float: Float): Scale {
                return when(float) {
                    1F -> DEFAULT
                    2F -> TWO
                    3F -> THREE
                    4F -> FOUR
                    5F -> FIVE
                    else -> DEFAULT
                }
            }
        }
    }
}