import subprocess, sys

def find_mc_pid():
    for line in subprocess.check_output(["jps", "-l"], text=True).splitlines():
        pid, _, name = line.partition(" ")
        if any(s in name.lower() for s in ("net.minecraft", "knot", "launchwrapper")):
            return pid
    sys.exit("Nie znaleziono procesu Minecrafta (jps -l)")

profile = sys.argv[1] if len(sys.argv) > 1 else "forge-1.8.9"
subprocess.check_call(["java", "-jar", "injector/build/libs/injector.jar",
                       "payload/build/libs/payload.jar", find_mc_pid(), profile])
