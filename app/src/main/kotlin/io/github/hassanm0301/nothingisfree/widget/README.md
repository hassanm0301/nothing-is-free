# widget/

Home-screen widgets land here, built with Jetpack Glance (`androidx.glance:glance-appwidget`,
already in `gradle/libs.versions.toml`). See `docs/WIDGETS.md` for the plan.

Nothing's own widgets are the visual target: black or white rounded rectangles, Ndot numerals,
a single red accent, and dot-matrix readouts. Reuse `dotmatrix.DotMatrix` for any pixel art so
the same frame can be pushed to the Glyph Matrix.
