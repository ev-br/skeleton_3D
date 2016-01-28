#
# An ad-hoc helper for profiling. 
# Use, e.g. with yep:
#  http://scikit-learn.org/stable/developers/performance.html
#  http://fa.bianp.net/blog/2011/a-profiler-for-python-extensions/
# Need to `$ pip install yep` and `$ sudo apt-get install google-perftool` first
# and then 
#    $ python -m yep prof.py
#    $ google-pprof --text `which python` prof.py.prof
#
from skimage import io
from skel import compute_thin_image

bat = io.imread('data/bat/bat-cochlea-volume.tif')
compute_thin_image(bat)
