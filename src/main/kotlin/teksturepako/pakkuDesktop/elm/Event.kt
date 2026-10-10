/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.elm

/**
 * Marker for cross-cutting notifications observed via [Store.messages] / [LocalMessages].
 *
 * Not sealed — plugins define their own event types in separate modules. Host [Component]
 * message types may implement [Event] when plugins should react to them.
 */
interface Event
