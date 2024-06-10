package korlibs.io.serialization.xml.internal

@PublishedApi internal inline fun <T> List<T>.fastForEach(callback: (T) -> Unit) {
    var n = 0
    while (n < size) callback(this[n++])
}

@PublishedApi internal inline fun <T> Array<T>.fastForEach(callback: (T) -> Unit) {
    var n = 0
    while (n < size) callback(this[n++])
}
