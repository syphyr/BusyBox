# <a href="http://jaredrummler.github.io/BusyBox/" target="_blank">BusyBox for Android</a>

<img src="busybox-installer/src/main/res/mipmap-xxhdpi/ic_launcher.png" align="left" hspace="10" vspace="10">

**The most reliable and advanced BusyBox installer for Android.**

BusyBox is currently available on Google Play:

[![Get it on Google Play](
    http://steverichey.github.io/google-play-badge-svg/img/en_get.svg)](
    https://play.google.com/store/apps/details?id=com.jrummy.busybox.installer)

Please join this [Google+ community](https://plus.google.com/communities/113855814423561594889) to become a beta tester.

Downloads
---------

| ABI  | SIZE   | MD5                              | DOWNLOAD                                                                                                                                |
|------|--------|----------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------|
| ARM  | 912KB  | ccaf3bb1e7f41d1e401d124945df31c4 | [busybox 1.24.1](https://github.com/jaredrummler/BusyBox/blob/master/busybox-compiler/compiled-1.24.1/arm/static/bin/busybox?raw=true)  |
| x86  | 1224KB | 5956265be9e79931c0e884ec3870f38c | [busybox 1.24.1](https://github.com/jaredrummler/BusyBox/blob/master/busybox-compiler/compiled-1.24.1/x86/static/bin/busybox?raw=true)  |
| MIPS | 1972KB | 881657a52d66660e078fffc275d81a3e | [busybox 1.24.1](https://github.com/jaredrummler/BusyBox/blob/master/busybox-compiler/compiled-1.24.1/mips/static/bin/busybox?raw=true) |

Building
--------

BusyBox for Android has proprietary dependencies. You cannot build the project; it isn't fully open source. However, much of the code is available for your viewing pleasure. Contributions to localize the project are welcome and appreciated.

Applets
-------

```
[, [[, acpid, adjtimex, ar, arp, arping, ash, awk, base64, basename, bbconfig, beep, blkid,
blockdev, bootchartd, brctl, bunzip2, bzcat, bzip2, cal, cat, catv, chat, chattr, chgrp, chmod,
chown, chpst, chroot, chrt, chvt, cksum, clear, cmp, comm, cp, cpio, crond, crontab, cryptpw,
cttyhack, cut, date, dc, dd, deallocvt, depmod, devfsd, devmem, df, diff, dirname, dmesg, dnsd,
dnsdomainname, dos2unix, dpkg, dpkg-deb, du, dumpkmap, echo, ed, egrep, env, envdir, envuidgid,
ether-wake, expand, expr, fakeidentd, false, fatattr, fbset, fbsplash, fdflush, fdformat, fdisk,
fgconsole, fgrep, find, findfs, flash_lock, flash_unlock, flashcp, flock, fold, free, freeramdisk,
fsck, fsck.minix, fstrim, fsync, ftpd, ftpget, ftpput, fuser, getopt, grep, groups, gunzip, gzip,
halt, hd, hdparm, head, hexdump, hostname, httpd, hush, hwclock, id, ifconfig, ifdown, ifenslave,
ifplugd, ifup, inetd, init, inotifyd, insmod, install, ionice, iostat, ip, ipaddr, ipcalc, iplink,
iproute, iprule, iptunnel, kbd_mode, kill, killall, killall5, klogd, less, linux32, linux64,
linuxrc, ln, loadkmap, logger, logname, losetup, lpd, lpq, lpr, ls, lsattr, lsmod, lsof, lspci,
lsusb, lzcat, lzma, lzop, lzopcat, makedevs, makemime, man, md5sum, mdev, mesg, microcom, mkdir,
mkdosfs, mke2fs, mkfifo, mkfs.ext2, mkfs.minix, mkfs.reiser, mkfs.vfat, mknod, mkpasswd, mkswap,
mktemp, modinfo, modprobe, more, mount, mountpoint, mpstat, mt, mv, nameif, nanddump, nandwrite,
nbd-client, nc, netstat, nice, nmeter, nohup, nslookup, ntpd, od, openvt, patch, pgrep, pidof,
ping, ping6, pipe_progress, pivot_root, pkill, pmap, popmaildir, poweroff, powertop, printenv,
printf, ps, pscan, pstree, pwd, pwdx, raidautorun, rdate, rdev, readlink, readprofile, realpath,
reboot, reformime, renice, reset, resize, rev, rm, rmdir, rmmod, route, rpm, rpm2cpio, rtcwake,
run-parts, runsv, runsvdir, rx, script, scriptreplay, sed, sendmail, seq, setarch, setconsole,
setkeycodes, setlogcons, setserial, setsid, setuidgid, sh, sha1sum, sha256sum, sha3sum, sha512sum,
showkey, shuf, slattach, sleep, smemcap, softlimit, sort, split, start-stop-daemon, stat, strings,
stty, sum, sv, svlogd, swapoff, swapon, switch_root, sync, sysctl, tac, tail, tar, tcpsvd, tee,
telnet, telnetd, test, tftp, tftpd, time, timeout, top, touch, tr, traceroute, traceroute6, true,
truncate, tty, ttysize, tunctl, tune2fs, ubiattach, ubidetach, ubimkvol, ubirmvol, ubirsvol,
ubiupdatevol, udpsvd, uevent, umount, uname, uncompress, unexpand, uniq, unix2dos, unlink, unlzma,
unlzop, unxz, unzip, uptime, usleep, uudecode, uuencode, vconfig, vi, volname, watch, watchdog, wc,
wget, which, whoami, whois, xargs, xz, xzcat, yes, zcat, zcip
```

License
-------

Apache 2.0 where applicable. See LICENSE file for details.
