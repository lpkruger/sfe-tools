tar cvjf sshsfe-src.tar.bz2 --exclude IARPA_DB --exclude make\* --exclude make\* --exclude .\* ssh-src/include/ ssh-src/dropbear/ ssh-src/src/ ssh-src/sillylib/src/ ssh-src/md5* ssh-src/sha*
tar cvf sshsfe-bin-`arch`.tar -C ~/sfe/dropbear dropbear dbclient
tar rvf sshsfe-bin-`arch`.tar -C ssh-src/ $(cd ssh-src/ ; echo md5* sha* )
rm sshsfe-bin-`arch`.tar.bz2
bzip2 sshsfe-bin-`arch`.tar
