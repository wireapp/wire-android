#!/usr/bin/env python

import os
import paramiko
import socket
import sys
import time
import tempfile

NODE_USER = os.getenv("NODE_USER", 'jenkins')
NODE_PASSWORD = os.getenv('NODE_PASSWORD', '123456')
MAX_NODES_PER_HOST = 5
CURRENT_HOST_IP = socket.gethostbyname(socket.gethostname())


def get_current_vm_number():
    ip_as_list = CURRENT_HOST_IP.split('.')
    # Must be IPv4
    assert len(ip_as_list) == 4
    last_digit = int(ip_as_list[-1])
    return last_digit % MAX_NODES_PER_HOST


POWER_CYCLE_SCRIPT = \
"""import time

from brainstem import discover
from brainstem.link import Spec
from brainstem.stem import USBHub2x4


stem = USBHub2x4()
spec = discover.find_first_module(Spec.USB)
if spec is None:
    raise RuntimeError("No USBHub is connected!")
stem.connect_from_spec(spec)
stem.usb.setPowerDisable({0})
time.sleep(5)
stem.usb.setPowerEnable({0})
time.sleep(1)
""".format(get_current_vm_number() - 1)
# VM number starts from 1


def calc_vm_master_host_ip():
    ip_as_list = CURRENT_HOST_IP.split('.')
    # Must be IPv4
    assert len(ip_as_list) == 4
    last_digit = int(ip_as_list[-1])
    return '.'.join(ip_as_list[:3] + [str(last_digit - (last_digit % MAX_NODES_PER_HOST))])


if __name__ == '__main__':
    client = paramiko.SSHClient()
    try:
        client.load_system_host_keys()
        client.set_missing_host_key_policy(paramiko.WarningPolicy())
        client.connect(calc_vm_master_host_ip(), username=NODE_USER, password=NODE_PASSWORD)
        sftp = client.open_sftp()
        _, localpath = tempfile.mkstemp(suffix='.py')
        try:
            with open(localpath, 'w') as f:
                f.write(POWER_CYCLE_SCRIPT)
            remotepath = '/tmp/' + os.path.basename(localpath)
            sftp.put(localpath, remotepath)
            sftp.close()
        finally:
            os.unlink(localpath)
        stdin, stdout, stderr = client.exec_command('python "{}"'.format(remotepath))
        stdout.channel.recv_exit_status()
        if stderr:
            sys.stderr.write('\n' + stderr.read() + '\n')
        client.exec_command('rm -f "{}"'.format(remotepath))
    finally:
        client.close()
