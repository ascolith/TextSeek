package android.net

import android.os.Parcel

/**
 * A [Uri] usable from JVM unit tests.
 *
 * The static factories (`Uri.parse`, `Uri.fromFile`) are stubs that throw "not mocked" off-device,
 * and `Uri.EMPTY` is null, so a real Uri cannot be obtained in a unit test. `Uri`'s constructor is
 * package-private rather than private, though, which makes a subclass legal — from this package
 * only. That is the sole reason this file lives under `android.net` instead of with the tests that
 * use it.
 *
 * Only the members a test is likely to touch return anything meaningful; the rest are null. Note
 * that `equals`/`hashCode` are overridden because `Uri`'s own implementations are stubs that would
 * throw if a test compared two instances.
 */
class FakeUri(private val value: String) : Uri() {

    private val afterScheme = value.substringAfter("://", "")

    override fun toString(): String = value

    override fun getScheme(): String? = value.substringBefore("://").ifEmpty { null }

    override fun getAuthority(): String? = afterScheme.substringBefore("/").ifEmpty { null }

    override fun getPath(): String? =
        afterScheme.substringAfter("/", "").ifEmpty { null }?.let { "/$it" }

    override fun getPathSegments(): MutableList<String> =
        afterScheme.substringAfter("/", "").split("/").filter { it.isNotEmpty() }.toMutableList()

    override fun getLastPathSegment(): String? = pathSegments.lastOrNull()

    override fun getEncodedAuthority(): String? = authority

    override fun getEncodedPath(): String? = path

    override fun isHierarchical(): Boolean = true

    override fun isRelative(): Boolean = false

    override fun getPort(): Int = -1

    override fun getFragment(): String? = null
    override fun getEncodedFragment(): String? = null
    override fun getQuery(): String? = null
    override fun getEncodedQuery(): String? = null
    override fun getSchemeSpecificPart(): String? = null
    override fun getEncodedSchemeSpecificPart(): String? = null
    override fun getUserInfo(): String? = null
    override fun getEncodedUserInfo(): String? = null
    override fun getHost(): String? = authority

    override fun buildUpon(): Builder = throw UnsupportedOperationException("FakeUri is immutable")

    override fun describeContents(): Int = 0

    override fun writeToParcel(dest: Parcel, flags: Int) = Unit

    override fun equals(other: Any?): Boolean = other is FakeUri && other.value == value

    override fun hashCode(): Int = value.hashCode()
}
