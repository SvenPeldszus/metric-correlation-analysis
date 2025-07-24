import json

import os
from os import listdir

import re
import shutil
import string

import paths as paths

keywords = ['security', 'crypt', 'auth', 'vulnerab', 'exploit', 'cve', 'attack', 
            'threat', 'malware', 'phishing', 'breach']

print('input: ' + os.path.abspath(paths.feat_req_folder))
print('output: ' + os.path.abspath(paths.feat_req_pot_security_folder))

for file in os.listdir(paths.feat_req_folder):
    os.makedirs(paths.feat_req_pot_security_folder, exist_ok=True)
    with open(paths.feat_req_folder + file) as f:
        json_content = json.load(f)
        text = (json_content.get('title')+'\n\n'+json_content.get('description')).lower()
        for keyword in keywords:
            if keyword in text:
                shutil.move(paths.feat_req_folder + file, paths.feat_req_pot_security_folder)
                break
