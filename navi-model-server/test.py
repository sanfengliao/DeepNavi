import os
from navi.test import test_deepnavi

test_deepnavi(os.path.basename(__file__), 'navi/model_weights/sc/sc.pth.tar')