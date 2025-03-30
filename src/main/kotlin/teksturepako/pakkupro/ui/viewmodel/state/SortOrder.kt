package teksturepako.pakkupro.ui.viewmodel.state

sealed class SortOrder(open val ascending: Boolean)
{
    data class Name(override val ascending: Boolean) : SortOrder(ascending)
    data class LastUpdated(override val ascending: Boolean) : SortOrder(ascending)
}