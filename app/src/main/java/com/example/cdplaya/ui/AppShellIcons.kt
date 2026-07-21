package com.example.cdplaya.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

object AppShellIcons {
    val Deck: ImageVector by lazy {
        shellIcon("CDPlayaDeck") {
            strokedPath {
                moveTo(4.5f, 7f)
                curveTo(3.7f, 7f, 3f, 7.7f, 3f, 8.5f)
                verticalLineTo(17.5f)
                curveTo(3f, 18.3f, 3.7f, 19f, 4.5f, 19f)
                horizontalLineTo(19.5f)
                curveTo(20.3f, 19f, 21f, 18.3f, 21f, 17.5f)
                verticalLineTo(8.5f)
                curveTo(21f, 7.7f, 20.3f, 7f, 19.5f, 7f)
                close()

                moveTo(3.5f, 10.5f)
                horizontalLineTo(20.5f)

                moveTo(7f, 14.75f)
                horizontalLineTo(10.5f)

                moveTo(17.75f, 14.75f)
                curveTo(17.75f, 15.72f, 16.97f, 16.5f, 16f, 16.5f)
                curveTo(15.03f, 16.5f, 14.25f, 15.72f, 14.25f, 14.75f)
                curveTo(14.25f, 13.78f, 15.03f, 13f, 16f, 13f)
                curveTo(16.97f, 13f, 17.75f, 13.78f, 17.75f, 14.75f)
                close()
            }
        }
    }

    val AlbumStack: ImageVector by lazy {
        shellIcon("CDPlayaAlbumStack") {
            strokedPath {
                moveTo(6f, 4f)
                horizontalLineTo(18f)
                curveTo(19.1f, 4f, 20f, 4.9f, 20f, 6f)
                verticalLineTo(18f)
                curveTo(20f, 19.1f, 19.1f, 20f, 18f, 20f)
                horizontalLineTo(6f)
                curveTo(4.9f, 20f, 4f, 19.1f, 4f, 18f)
                verticalLineTo(6f)
                curveTo(4f, 4.9f, 4.9f, 4f, 6f, 4f)
                close()

                moveTo(8f, 1.75f)
                horizontalLineTo(18f)

                moveTo(2f, 8f)
                verticalLineTo(18f)

                moveTo(15.25f, 12f)
                curveTo(15.25f, 13.8f, 13.8f, 15.25f, 12f, 15.25f)
                curveTo(10.2f, 15.25f, 8.75f, 13.8f, 8.75f, 12f)
                curveTo(8.75f, 10.2f, 10.2f, 8.75f, 12f, 8.75f)
                curveTo(13.8f, 8.75f, 15.25f, 10.2f, 15.25f, 12f)
                close()

                moveTo(12f, 11.6f)
                verticalLineTo(12.4f)
            }
        }
    }

    val Search: ImageVector by lazy {
        shellIcon("CDPlayaSearch") {
            strokedPath {
                moveTo(16.75f, 10.75f)
                curveTo(16.75f, 14.06f, 14.06f, 16.75f, 10.75f, 16.75f)
                curveTo(7.44f, 16.75f, 4.75f, 14.06f, 4.75f, 10.75f)
                curveTo(4.75f, 7.44f, 7.44f, 4.75f, 10.75f, 4.75f)
                curveTo(14.06f, 4.75f, 16.75f, 7.44f, 16.75f, 10.75f)
                close()

                moveTo(15.25f, 15.25f)
                lineTo(20f, 20f)
            }
        }
    }

    val GridView: ImageVector by lazy {
        shellIcon("CDPlayaGridView") {
            strokedPath {
                moveTo(4.5f, 4.5f)
                horizontalLineTo(10f)
                verticalLineTo(10f)
                horizontalLineTo(4.5f)
                close()

                moveTo(14f, 4.5f)
                horizontalLineTo(19.5f)
                verticalLineTo(10f)
                horizontalLineTo(14f)
                close()

                moveTo(4.5f, 14f)
                horizontalLineTo(10f)
                verticalLineTo(19.5f)
                horizontalLineTo(4.5f)
                close()

                moveTo(14f, 14f)
                horizontalLineTo(19.5f)
                verticalLineTo(19.5f)
                horizontalLineTo(14f)
                close()
            }
        }
    }

    val ListView: ImageVector by lazy {
        shellIcon("CDPlayaListView") {
            strokedPath {
                moveTo(5f, 6.5f)
                horizontalLineTo(7f)
                verticalLineTo(8.5f)
                horizontalLineTo(5f)
                close()
                moveTo(10f, 7.5f)
                horizontalLineTo(19f)

                moveTo(5f, 11f)
                horizontalLineTo(7f)
                verticalLineTo(13f)
                horizontalLineTo(5f)
                close()
                moveTo(10f, 12f)
                horizontalLineTo(19f)

                moveTo(5f, 15.5f)
                horizontalLineTo(7f)
                verticalLineTo(17.5f)
                horizontalLineTo(5f)
                close()
                moveTo(10f, 16.5f)
                horizontalLineTo(19f)
            }
        }
    }

    private fun shellIcon(
        name: String,
        paths: ImageVector.Builder.() -> Unit
    ): ImageVector {
        return ImageVector.Builder(
            name = name,
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply(paths).build()
    }

    private fun ImageVector.Builder.strokedPath(
        commands: androidx.compose.ui.graphics.vector.PathBuilder.() -> Unit
    ) {
        path(
            fill = null,
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.8f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathBuilder = commands
        )
    }
}
