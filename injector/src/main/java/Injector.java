import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.VirtualMachineDescriptor;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

public class Injector {
    public static void main(String[] a) throws Exception {
        if (a.length < 1) {
            System.out.println("Uzycie: java -jar injector.jar <payload.jar> [pid] [profil_mappingow]");
            VirtualMachine.list().forEach(d -> System.out.println(d.id() + "  " + d.displayName()));
            return;
        }
        String payload = new File(a[0]).getAbsolutePath();
        String profile = a.length > 2 ? a[2] : "forge-1.8.9";

        String pid;
        if (a.length > 1) {
            pid = a[1];
        } else {
            List<VirtualMachineDescriptor> mc = VirtualMachine.list().stream()
                    .filter(d -> {
                        String n = d.displayName().toLowerCase();
                        return n.contains("net.minecraft") || n.contains("knot") || n.contains("launchwrapper");
                    }).collect(Collectors.toList());
            if (mc.size() != 1) {
                System.out.println("Nie znaleziono jednoznacznie procesu MC, podaj pid. Kandydaci: " + mc.size());
                VirtualMachine.list().forEach(d -> System.out.println(d.id() + "  " + d.displayName()));
                return;
            }
            pid = mc.get(0).id();
        }

        VirtualMachine vm = VirtualMachine.attach(pid);
        try {
            try {
                vm.loadAgent(payload, profile);
            } catch (com.sun.tools.attach.AgentLoadException e) {
                // JDK 9+ -> JVM 8: "0" oznacza sukces (niezgodny format odpowiedzi)
                if (!"0".equals(e.getMessage())) throw e;
            }
            System.out.println("Wstrzykniete do PID " + pid);
        } finally {
            vm.detach();
        }
    }
}
