Render deployment for this repository should use the root `Dockerfile`.

Why:
- The Java app already builds correctly via Maven in Docker.
- Frontend SCSS files are development sources and should not be compiled by Render buildpacks.
- Using Docker avoids accidental Node/Sass build detection such as failures caused by SCSS syntax or toolchain differences.

Expected Render configuration:
- Runtime: `Docker`
- Dockerfile: `./Dockerfile`
- Root directory: repository root

If a Render service is currently configured as a native Node or buildpack service, switch it to a Docker service or recreate it from `render.yaml`.
