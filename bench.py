#
# An ad-hoc benchmark. Use via `$ ipython bench.py`
#
from IPython import get_ipython
ipython = get_ipython()

from skimage import io
from skel import compute_thin_image

fname = 'data/bat/bat-cochlea-volume.tif'
bat = io.imread(fname)
compute_thin_image(bat)

ipython.run_line_magic("timeit", "compute_thin_image(bat)")
