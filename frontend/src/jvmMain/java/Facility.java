import me.cpele.facility.core.programs.SlackAccount;
import me.cpele.facility.shell.SlackAccountExtKt;

public class Facility {
    public static void main(String[] args) {
        SlackAccountExtKt.main(SlackAccount.INSTANCE, args);
    }
}
