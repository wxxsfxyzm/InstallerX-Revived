// Copyright 2026, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0

package com.rosan.installer.ui.library.blend

class BlurToken {
    class BlendColor {
        object Dark {
            val DEFAULT: IntArray = intArrayOf(1970500467, -1977211354)
            val EXTRA_HEAVY: IntArray = intArrayOf(1970500467, -1979711488, 184549375)
            val HEAVY: IntArray = intArrayOf(-2141430692, -1088479457)
            val LIGHT: IntArray = intArrayOf(1636469386, 1296187970)
            val EXTRA_LIGHT: IntArray = intArrayOf(1303227821, 862019937)
        }

        object Light {
            val DEFAULT: IntArray = intArrayOf(-1889443744, -1544359182)
            val EXTRA_HEAVY: IntArray = intArrayOf(-1889443744, -1543503873)
            val HEAVY: IntArray = intArrayOf(-1502909589, -856295947)
            val LIGHT: IntArray = intArrayOf(-2057807784, 1088676835)
            val EXTRA_LIGHT: IntArray = intArrayOf(-2142417587, 651811289)
        }
    }

    class BlendMode {
        object Dark {
            val DEFAULT: IntArray = intArrayOf(19, 3, 3)
        }

        object Light {
            val DEFAULT: IntArray = intArrayOf(18, 3)
        }
    }

    object Effect {
        const val DEFAULT: Int = 66
        const val EXTRA_THIN: Int = 30
        const val HEAVY: Int = 74
        const val THIN: Int = 52
    }
}
