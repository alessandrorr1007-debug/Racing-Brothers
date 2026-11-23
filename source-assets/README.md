# Blender source assets

Authoring sources for the four cars and four pilots. These files are **not** part of the Android
APK: only the exported GLB files under `app/src/main/assets/models/` are packaged.

```
vehicles/   Bugatti / Ferrari / Lamborghini / McLaren .blend + preview PNG
pilots/     Pilot_01 .. Pilot_04 .blend + preview PNG
backups/    antes_reconstruccion_2026-11-23/ - original pre-rebuild car models
```

Exported GLB conventions:

- Vehicles: `Body` + `Wheel_Front_L/R`, `Wheel_Rear_L/R` nodes; `base` = LOD1,
  `_LOD0` = high detail, `_LOD2` = distance variant.
- Pilots: skinned mesh with `Driving` and `Exhibition` animation clips.
- Wire IDs stay stable (`vortex_rs`, `apex_gtx`, `titan_v8`, `formula_aero`;
  `nico_turbo`, `kira_apex`, `beto_torque`, `pilot_04`) so existing saves and rooms keep working.
