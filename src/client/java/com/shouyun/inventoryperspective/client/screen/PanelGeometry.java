package com.shouyun.inventoryperspective.client.screen;

import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Matrix4d;
import org.joml.Vector4d;
import org.joml.Vector4f;

/** One frame's world plane and camera, shared by drawing and mouse ray intersection. */
public record PanelGeometry(Vec3 center, Vec3 right, double unit, int width, int height,
                            Vec3 camera, Matrix4f worldToClip) {
    public record Point(double x, double y) {}

    public Vec3 worldPoint(double guiX, double guiY) {
        return center.add(right.scale((guiX - width / 2.0) * unit))
                .add(0, (height / 2.0 - guiY) * unit, 0);
    }

    public Point project(double guiX, double guiY) {
        Vec3 relative = worldPoint(guiX, guiY).subtract(camera);
        Vector4f clip = worldToClip.transform(new Vector4f((float) relative.x, (float) relative.y, (float) relative.z, 1));
        if (clip.w <= 0) return null;
        return new Point((clip.x / clip.w + 1) * width / 2.0, (1 - clip.y / clip.w) * height / 2.0);
    }

    public Point intersect(double screenX, double screenY) {
        Matrix4d inverse = new Matrix4d(worldToClip).invert();
        double x = 2 * screenX / width - 1;
        double y = 1 - 2 * screenY / height;
        Vector4d near = inverse.transform(new Vector4d(x, y, -1, 1));
        Vector4d far = inverse.transform(new Vector4d(x, y, 1, 1));
        Vec3 origin = camera.add(near.x / near.w, near.y / near.w, near.z / near.w);
        Vec3 direction = camera.add(far.x / far.w, far.y / far.w, far.z / far.w).subtract(origin).normalize();
        Vec3 normal = right.cross(new Vec3(0, 1, 0));
        double denominator = direction.dot(normal);
        if (Math.abs(denominator) < 0.00001) return null;
        double distance = center.subtract(origin).dot(normal) / denominator;
        if (distance <= 0) return null;
        Vec3 offset = origin.add(direction.scale(distance)).subtract(center);
        double guiX = width / 2.0 + offset.dot(right) / unit;
        double guiY = height / 2.0 - offset.y / unit;
        return guiX >= 0 && guiX < width && guiY >= 0 && guiY < height ? new Point(guiX, guiY) : null;
    }
}
