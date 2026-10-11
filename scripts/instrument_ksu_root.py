#!/usr/bin/env python3
"""Read-only KernelSU root-path instrumentation for the experimental Sultan build.

This does NOT grant root, bypass manager identity checks, disable seccomp, or
change the SUSFS v2.0.0 patch. Use the counters after boot to distinguish
manager recognition, zygote hooks, driver FD installation and GRANT_ROOT.
"""
from pathlib import Path
import sys

root = Path(sys.argv[1])
ksu = root / "KernelSU-Next" / "kernel"


def replace(relative, old, new, count=1):
    path = ksu / relative
    src = path.read_text()
    found = src.count(old)
    if found != count:
        raise RuntimeError(f"{relative}: expected {count} occurrences, got {found}: {old!r}")
    path.write_text(src.replace(old, new, count))


replace(
    "hook/setuid_hook.c",
    '#include "hook/setuid_hook.h"',
    '#include "hook/setuid_hook.h"\n#include <linux/moduleparam.h>',
)
replace(
    "hook/setuid_hook.c",
    "extern struct cred *ksu_cred;",
    """extern struct cred *ksu_cred;

/* Read-only; exposed under /sys/module/kernelsu/parameters/ when available. */
static unsigned int ksu_diag_manager_setresuid;
static unsigned int ksu_diag_manager_sid_hit;
static unsigned int ksu_diag_manager_fd_failed;
module_param_named(diag_manager_setresuid, ksu_diag_manager_setresuid, uint, 0444);
module_param_named(diag_manager_sid_hit, ksu_diag_manager_sid_hit, uint, 0444);
module_param_named(diag_manager_fd_failed, ksu_diag_manager_fd_failed, uint, 0444);""",
)

# The first two are the SUSFS zygote/zygote_next branches, while the third
# belongs to the #else non-SUSFS implementation. Retain its original code.
old_fd = "        ksu_install_fd();\n        return 0;"
path = ksu / "hook/setuid_hook.c"
src = path.read_text()
if src.count(old_fd) != 3:
    raise RuntimeError(f"Expected 3 manager FD branches, found {src.count(old_fd)}")
src = src.replace(
    old_fd,
    "        if (ksu_install_fd() < 0)\n"
    "            ksu_diag_manager_fd_failed++;\n"
    "        return 0;",
    2,
)
path.write_text(src)

replace(
    "hook/setuid_hook.c",
    "    if (cur_uid != 0)\n        return 0;\n\n"
    "    // We only interest in process spwaned by zygote or zygote_next",
    "    if (cur_uid != 0)\n        return 0;\n\n"
    "    if (ksu_is_manager_appid_valid() && is_uid_manager(ruid))\n"
    "        ksu_diag_manager_setresuid++;\n\n"
    "    // We only interest in process spwaned by zygote or zygote_next",
)
replace(
    "hook/setuid_hook.c",
    "    if (susfs_is_sid_equal(current_cred(), susfs_zygote_sid))\n"
    "        return handle_zygote_setresuid(ruid);\n\n"
    "    if (susfs_is_sid_equal(current_cred(), susfs_zygote_next_sid))\n"
    "        return handle_zygote_next_setresuid(ruid);",
    "    if (susfs_is_sid_equal(current_cred(), susfs_zygote_sid)) {\n"
    "        if (ksu_is_manager_appid_valid() && is_uid_manager(ruid))\n"
    "            ksu_diag_manager_sid_hit++;\n"
    "        return handle_zygote_setresuid(ruid);\n"
    "    }\n\n"
    "    if (susfs_is_sid_equal(current_cred(), susfs_zygote_next_sid)) {\n"
    "        if (ksu_is_manager_appid_valid() && is_uid_manager(ruid))\n"
    "            ksu_diag_manager_sid_hit++;\n"
    "        return handle_zygote_next_setresuid(ruid);\n"
    "    }",
)

replace(
    "supercall/dispatch.c",
    '#include "supercall/supercall.h"',
    '#include "supercall/supercall.h"\n#include <linux/moduleparam.h>',
)
replace(
    "supercall/dispatch.c",
    "static int do_grant_root(void __user *arg)",
    """/* Diagnostic counters only; original authorization gates remain intact. */
static unsigned int ksu_diag_root_grant_attempts;
static unsigned int ksu_diag_root_grant_denied;
static unsigned int ksu_diag_root_grant_failed;
module_param_named(diag_root_grant_attempts, ksu_diag_root_grant_attempts, uint, 0444);
module_param_named(diag_root_grant_denied, ksu_diag_root_grant_denied, uint, 0444);
module_param_named(diag_root_grant_failed, ksu_diag_root_grant_failed, uint, 0444);
module_param_named(diag_manager_appid, ksu_manager_appid, uint, 0444);

static int do_grant_root(void __user *arg)""",
)
replace(
    "supercall/dispatch.c",
    '    pr_info("allow root for: %d\\n", audit_uid);\n'
    "    ret = escape_with_root_profile();",
    '    ksu_diag_root_grant_attempts++;\n'
    '    pr_info("allow root for: %d\\n", audit_uid);\n'
    "    ret = escape_with_root_profile();\n"
    "    if (ret)\n"
    "        ksu_diag_root_grant_failed++;",
)
replace(
    "supercall/dispatch.c",
    '                pr_warn("ksu ioctl: permission denied for cmd=0x%x uid=%d\\n", cmd, current_uid().val);\n'
    "                return -EPERM;",
    "                if (cmd == KSU_IOCTL_GRANT_ROOT)\n"
    "                    ksu_diag_root_grant_denied++;\n"
    '                pr_warn("ksu ioctl: permission denied for cmd=0x%x uid=%d\\n", cmd, current_uid().val);\n'
    "                return -EPERM;",
)

# Preserve the known-good KernelSU Next <-> historical SUSFS reboot ABI bridge.
dispatch = (ksu / "supercall/dispatch.c").read_text()
assert "int ksu_handle_sys_reboot(" in dispatch
assert "ksu_supercall_reboot_handler(arg)" in dispatch

checks = {
    "kernel/reboot.c": ["ksu_handle_sys_reboot(", "CONFIG_KSU_SUSFS"],
    "kernel/sys.c": ["ksu_handle_setresuid(", "CONFIG_KSU_SUSFS"],
    "fs/exec.c": ["ksu_handle_execveat(", "is_first_zygote"],
    "KernelSU-Next/kernel/supercall/dispatch.c": [
        "ksu_diag_root_grant_denied", "ksu_diag_root_grant_attempts",
    ],
    "KernelSU-Next/kernel/hook/setuid_hook.c": [
        "ksu_diag_manager_setresuid", "ksu_diag_manager_sid_hit",
        "ksu_diag_manager_fd_failed",
    ],
}
for filename, needles in checks.items():
    src = (root / filename).read_text()
    for needle in needles:
        assert needle in src, (filename, needle)
assert '#define SUSFS_VERSION "v2.0.0"' in (root / "include/linux/susfs.h").read_text()
print("Root auth diagnostics installed; SUSFS v2.0.0 and manual syscall hooks intact")
