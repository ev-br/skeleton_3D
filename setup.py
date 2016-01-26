#!/usr/bin/env python

import os
from skimage._build import cython

base_path = os.path.abspath(os.path.dirname(__file__))


def configuration(parent_package='', top_path=None):
    from numpy.distutils.misc_util import Configuration, get_numpy_include_dirs

    config = Configuration('skeletonize_3d', parent_package, top_path)

    cython(['_skel.pyx'], working_path=base_path)
    config.add_extension('_skel', sources=['_skel.c'],
                         include_dirs=[get_numpy_include_dirs()])
    return config

if __name__ == '__main__':
    from numpy.distutils.core import setup
    setup(**(configuration(top_path='').todict()))
