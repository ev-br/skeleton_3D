#
# An ad-hoc benchmark. Use via `$ ipython bench.py`
#
from IPython import get_ipython
ipython = get_ipython()

from skimage import io
from skel import compute_thin_image

fname = 'data/bat/bat-cochlea-volume.tif'
bat = io.imread(fname)
ipython.run_line_magic("timeit", "compute_thin_image(bat)")


#import numpy as np
#raw = np.fromfile('../lobster.raw', dtype=np.uint8)
#raw = raw.reshape(301, 324, 56)
#ipython.run_line_magic("timeit", "compute_thin_image(raw)")
