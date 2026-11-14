import re
import sys
from pathlib import Path

task_file, payload_file, out_dir = (Path(a) for a in sys.argv[1:4])
text = task_file.read_text(encoding="utf-8")
if not re.search(r"^abstract class EncryptProPayload", text, re.M):
    sys.exit("EncryptProPayload not found in " + str(task_file))
(out_dir / "payload").mkdir(parents=True, exist_ok=True)
(out_dir / "payload" / "pro_filters.txt").write_text(payload_file.read_text(encoding="utf-8"), encoding="utf-8")
(out_dir / "settings.gradle.kts").write_text('rootProject.name = "pro-payload-check"\n', encoding="utf-8")
registrations = """
fun TaskContainer.sealWith(name: String, cert: String, dir: String) = register<EncryptProPayload>(name) {
    source.set(layout.projectDirectory.file("payload/pro_filters.txt"))
    certSha256.set(cert)
    outputDir.set(layout.buildDirectory.dir(dir))
}
tasks.sealWith("sealed", providers.gradleProperty("cert").get(), "sealed")
tasks.sealWith("plain", "", "plain")
tasks.sealWith("badCert", "abc123", "bad")
"""
(out_dir / "build.gradle.kts").write_text(text + registrations, encoding="utf-8")
