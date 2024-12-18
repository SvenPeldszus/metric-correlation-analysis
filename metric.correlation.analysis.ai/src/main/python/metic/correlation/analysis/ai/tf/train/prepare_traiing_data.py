import json

import os
from os import listdir

import re
import shutil
import string

import main.python.metic.correlation.analysis.ai.tf.train.paths as paths

os.makedirs(paths.ng_folder, exist_ok=True)

print('list: '+os.path.abspath(paths.security_feat_req_folder))

for file in os.listdir(paths.security_feat_req_folder):
    folder = paths.training_folder + 'security/'
    os.makedirs(folder, exist_ok=True)
    with open(paths.security_feat_req_folder + file) as f:
        json_content = json.load(f)
        with open(folder+json_content.get('id')+'.txt', 'w') as txt:
            text = json_content.get('title')+'\n\n'+json_content.get('description')
            txt.write(text)

for file in os.listdir(paths.feat_req_folder):
    folder = paths.training_folder + 'other/'
    os.makedirs(folder, exist_ok=True)
    with open(paths.feat_req_folder + file) as f:
        json_content = json.load(f)
        with open(folder+json_content.get('id')+'.txt', 'w') as txt:
            text = json_content.get('title')+'\n\n'+json_content.get('description')
            txt.write(text)