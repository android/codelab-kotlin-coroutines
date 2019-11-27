package com.example.android.advancedcoroutines.utils

data class ComparablePair<A: Comparable<A>, B: Comparable<B>>(
    val first: A,
    val second: B
) : Comparable<ComparablePair<A, B>> {
    override fun compareTo(other: ComparablePair<A, B>): Int {
        val firstComp = this.first.compareTo(other.first)
        if (firstComp != 0) { return firstComp }
        return this.second.compareTo(other.second)
    }
}