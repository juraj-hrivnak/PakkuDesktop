/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.pro.git

fun longestCommonSubsequence(a: List<String>, b: List<String>): List<String>
{
    val lengths = Array(a.size + 1) { IntArray(b.size + 1) }

    a.indices.fold(lengths) { acc, i ->
        b.indices.fold(acc) { innerAcc, j ->
            innerAcc.also {
                it[i + 1][j + 1] = if (a[i] == b[j])
                {
                    it[i][j] + 1
                }
                else
                {
                    maxOf(it[i + 1][j], it[i][j + 1])
                }
            }
        }
    }

    tailrec fun buildLCS(
        i: Int,
        j: Int,
        result: List<String>,
    ): List<String> = when
    {
        i <= 0 || j <= 0                      -> result
        a[i - 1] == b[j - 1]                  -> buildLCS(i - 1, j - 1, listOf(a[i - 1]) + result)
        lengths[i - 1][j] > lengths[i][j - 1] -> buildLCS(i - 1, j, result)
        else                                  -> buildLCS(i, j - 1, result)
    }

    return buildLCS(a.size, b.size, emptyList())
}