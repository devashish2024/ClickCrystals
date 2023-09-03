package io.github.itzispyder.clickcrystals.util.misc;

import io.github.itzispyder.clickcrystals.data.Pair;
import io.github.itzispyder.clickcrystals.util.ChatUtils;
import io.github.itzispyder.clickcrystals.util.MathUtils;
import io.github.itzispyder.clickcrystals.util.PlayerUtils;
import io.github.itzispyder.clickcrystals.util.Randomizer;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class CameraRotator {

    private static final AtomicBoolean running = new AtomicBoolean(false);
    private static boolean cursorLocked = false;
    private final EndAction onFinish;
    private final List<Goal> goals;
    private final AtomicReference<Goal> currentGoal;
    private final AtomicBoolean skipRequestReceived, wasCancelled;
    protected boolean debugMessages, canLockCursor;

    private CameraRotator(List<Goal> goals, EndAction onFinish) {
        this.onFinish = onFinish;
        this.goals = goals;
        this.currentGoal = new AtomicReference<>();
        skipRequestReceived = wasCancelled = new AtomicBoolean(false);
    }

    public static boolean isCameraRunning() {
        return running.get();
    }

    public static boolean isCursorLocked() {
        return cursorLocked;
    }

    public static synchronized void lockCursor() {
        cursorLocked = true;
    }

    public static synchronized void unlockCursor() {
        cursorLocked = false;
    }

    public boolean canLockCursor() {
        return canLockCursor;
    }

    public boolean cursorLocked() {
        return cursorLocked;
    }

    public boolean isRunning() {
        return running.get();
    }

    public boolean wasCancelled() {
        return wasCancelled.get();
    }

    public AtomicReference<Goal> getCurrentGoal() {
        return currentGoal;
    }

    public EndAction getEndAction() {
        return onFinish;
    }

    public void cancel() {
        running.set(false);
        wasCancelled.set(true);
    }

    public void clearGoals() {
        cancel();
        goals.clear();
    }

    public void addGoal(Goal goal) {
        if (isRunning()) {
            throw new IllegalArgumentException("Tried to squeeze in a goal when camera is already running.");
        }
        goals.add(goal);
    }

    public void skip() {
        skipRequestReceived.set(true);
        if (goals.size() <= 1) {
            wasCancelled.set(true);
        }
    }

    public void start() {
        if (isRunning()) {
            if (debugMessages) {
                ChatUtils.sendPrefixMessage("§cA camera rotator is already active!");
            }
            return;
        }
        wasCancelled.set(false);
        running.set(true);

        CompletableFuture.runAsync(() -> {
            if (canLockCursor()) {
                lockCursor();
            }

            try {
                for (Goal goal : goals) {
                    if (!isRunning()) break;
                    setGoalAndTarget(goal);
                }
            }
            catch (ConcurrentModificationException ignore) {}

            if (cursorLocked) {
                unlockCursor();
            }

            ClientPlayerEntity p = PlayerUtils.player();
            running.set(false);
            onFinish.accept(MathUtils.wrapDegrees(p.getPitch()), MathUtils.wrapDegrees(p.getYaw()), this);
        });
    }

    private synchronized void setGoalAndTarget(Goal goal) {
        if (PlayerUtils.playerNotNull()) {
            currentGoal.set(goal);
            Goal rot = getRotation();

            if (rot.pitch == goal.pitch && rot.yaw == goal.yaw) {
                if (debugMessages) {
                    ChatUtils.sendPrefixMessage( rot.pitch + " is already " + goal.pitch + ", and " +  rot.yaw + " is already " + goal.yaw);
                }
                skipRequestReceived.set(false);
                return;
            }

            int deltaPitch = (int)MathUtils.angleBetween(rot.pitch, goal.pitch); // vertical
            int deltaYaw = (int)MathUtils.angleBetween(rot.yaw, goal.yaw); // plane
            boolean shouldPitchAdd = goal.pitch > rot.pitch;
            boolean shouldYawAdd;

            if ((rot.yaw < -90 && goal.yaw > 90)) { // crossing over the -180 to 179 border (right to left)
                shouldYawAdd = false;
            }
            else if ((rot.yaw > 90 && goal.yaw < -90)) { // crossing over the 179 to -180 border (left to right)
                shouldYawAdd = true;
            }
            else { // normal
                shouldYawAdd = goal.yaw > rot.yaw;
            }

            int progressPitch = 0;
            int progressYaw = 0;
            double dist = Math.sqrt(deltaPitch * deltaPitch + deltaYaw * deltaYaw);

            if (debugMessages) {
                ChatUtils.sendPrefixMessage("Targeting goal §7" + goal + "§f from §7" + rot + "§f, difference §7(" + deltaPitch + ", " + deltaYaw + ")");
            }

            while (isRunning() && !skipRequestReceived.get() && (progressPitch < deltaPitch || progressYaw < deltaYaw)) {
                if (progressPitch < deltaPitch) {
                    progressPitch++;
                    rot.pitch = rot.pitch + (shouldPitchAdd ? 1 : -1);
                    PlayerUtils.player().setPitch(rot.pitch);
                }
                if (progressYaw < deltaYaw) {
                    progressYaw++;
                    rot.yaw = rot.yaw + (shouldYawAdd ? 1 : -1);
                    PlayerUtils.player().setYaw(rot.yaw);
                }

                try {
                    long delay;
                    if (dist < 90.0) {
                        delay = Randomizer.rand(1, 6);
                    }
                    else if (dist < 180.0) {
                        delay = Randomizer.rand(1, 5);
                    }
                    else {
                        delay = Randomizer.rand(1, 3);
                    }
                    Thread.sleep(delay);
                }
                catch (Exception ignore) {}
            }

            if (debugMessages && !skipRequestReceived.get()) {
                ChatUtils.sendPrefixMessage("Done! Set your rotation to §7pitch=" + goal.pitch + ", yaw=" + goal.yaw);
            }
            skipRequestReceived.set(false);
        }
    }

    private synchronized Goal getRotation() {
        if (PlayerUtils.playerNotNull()) {
            float pitch = PlayerUtils.player().getPitch();
            float yaw = PlayerUtils.player().getYaw();
            return new Goal(pitch, yaw);
        }
        return new Goal(0, 0);
    }

    public static Builder create() {
        return new Builder();
    }

    public static class Builder {
        private final List<Goal> goals;
        private EndAction onFinish;
        private boolean debugMessages, canLockCursor;

        public Builder() {
            this.goals = new ArrayList<>();
            this.onFinish = (pitch, yaw, cameraRotator) -> {};
            debugMessages = canLockCursor = false;
        }

        public Builder enableDebug() {
            debugMessages = true;
            return this;
        }

        public Builder enableCursorLock() {
            canLockCursor = true;
            return this;
        }

        public Builder addGoal(Goal goal){
            if (goal != null) {
                goals.add(goal);
            }
            return this;
        }

        public Builder onFinish(EndAction onFinish) {
            this.onFinish = onFinish;
            return this;
        }

        public CameraRotator build() {
            boolean debug = debugMessages;
            boolean cursor = canLockCursor;
            return new CameraRotator(goals, onFinish) {{
                this.debugMessages = debug;
                this.canLockCursor = cursor;
            }};
        }
    }

    public static class Goal {
        private int pitch, yaw;

        public Goal(double pitch, double yaw) {
            this.pitch = (int)MathUtils.wrapDegrees(pitch);
            this.yaw = (int)MathUtils.wrapDegrees(yaw);
        }

        public Goal(double x, double y, double z) {
            Pair<Float, Float> polar = MathUtils.toPolar(x, y, z);
            float left = polar.left;
            float right = polar.right;
            this.pitch = (int)MathUtils.wrapDegrees(left);
            this.yaw = (int)MathUtils.wrapDegrees(right);
        }

        public Goal(Vec3d vec) {
            this(vec.x, vec.y, vec.z);
        }

        public final int getPitch() {
            return pitch;
        }

        public final int getYaw() {
            return yaw;
        }

        @Override
        public String toString() {
            return "pitchYaw(" + pitch + ", " + yaw + ")";
        }
    }
    
    @FunctionalInterface
    public interface EndAction {
        void accept(double pitch, double yaw, CameraRotator cameraRotator);
    }
}
