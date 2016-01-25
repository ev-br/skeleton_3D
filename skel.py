from __future__ import division, print_function, absolute_import

import numpy as np


def prepare_image(img_in):
    """Convert to a binary image, pad the it w/ zeros, and ensure it's 3D.
    """
    if img_in.ndim < 2 or img_in.ndim > 3:
        raise ValueError('expect 2D, got ndim = %s' % img_in.ndim)

    img = img_in.copy()

    if img.ndim == 2:
        img = img.reshape(img.shape + (1,))

    # normalize to binary
    img[img != 0] = 1

    # pad w/ zeros to simplify dealing w/ neighborhood of a pixel
    img_o = np.zeros(tuple(s + 2 for s in img.shape),
                     dtype=np.uint8)
    img_o[1:-1, 1:-1, 1:-1] = img.astype(np.uint8)

    return img_o


def postprocess_image(img_o):
    """Clip the image (padding is an implementation detail), convert to b/w.
    """
    img_oo = img_o[1:-1, 1:-1, 1:-1]
    img_oo = img_oo.squeeze()
    img_oo *= 255
    return img_oo


def get_neighborhood(img, x, y, z):
    """Get the neighborhood of a pixel.

       Assume zero boundary conditions. Image is already padded, so no
       out-of-bounds checking. 
    """
    neighborhood = np.zeros(27, dtype=np.uint8)

    neighborhood[0] = img[x-1, y-1, z-1]
    neighborhood[1] = img[x,   y-1, z-1]
    neighborhood[2] = img[x+1, y-1, z-1]

    neighborhood[ 3] = img[x-1, y, z-1]
    neighborhood[ 4] = img[x,   y, z-1]
    neighborhood[ 5] = img[x+1, y, z-1]

    neighborhood[ 6] = img[x-1, y+1, z-1]
    neighborhood[ 7] = img[x,   y+1, z-1]
    neighborhood[ 8] = img[x+1, y+1, z-1]

    neighborhood[ 9] = img[x-1, y-1, z]
    neighborhood[10] = img[x,   y-1, z]
    neighborhood[11] = img[x+1, y-1, z]

    neighborhood[12] = img[x-1, y, z]
    neighborhood[13] = img[x,   y, z]
    neighborhood[14] = img[x+1, y, z]

    neighborhood[15] = img[x-1, y+1, z]
    neighborhood[16] = img[x,   y+1, z]
    neighborhood[17] = img[x+1, y+1, z]

    neighborhood[18] = img[x-1, y-1, z+1]
    neighborhood[19] = img[x,   y-1, z+1]
    neighborhood[20] = img[x+1, y-1, z+1]

    neighborhood[21] = img[x-1, y, z+1]
    neighborhood[22] = img[x,   y, z+1]
    neighborhood[23] = img[x+1, y, z+1]

    neighborhood[24] = img[ x-1, y+1, z+1]
    neighborhood[25] = img[ x,   y+1, z+1]
    neighborhood[26] = img[ x+1, y+1, z+1]

    return neighborhood


### Neighborhood accessors. Keep them for now to mimic the ITK code.
def N(img, x, y, z):
    return img[x, y-1, z]

def S(img, x, y, z):
    return img[x, y+1, z]

def E(img, x, y, z):
    return img[x+1, y, z]

def W(img, x, y, z):
    return img[x-1, y, z]

def U(img, x, y, z):
    return img[x, y, z+1]

def B(img, x, y, z):
    return img[x, y, z-1]


###### look-up tables
def fill_numpoints_LUT(n=256):
    p = int(np.log2(n) + 1)
    return np.sum(np.arange(n)[:, None] & (1 << np.arange(p)) != 0, axis=1)

NUMPOINTS_LUT = fill_numpoints_LUT()


def fill_Euler_LUT():
    LUT = np.zeros(256, dtype=int)

    LUT[1]  =  1
    LUT[3]  = -1
    LUT[5]  = -1
    LUT[7]  =  1
    LUT[9]  = -3
    LUT[11] = -1
    LUT[13] = -1
    LUT[15] =  1
    LUT[17] = -1
    LUT[19] =  1
    LUT[21] =  1
    LUT[23] = -1
    LUT[25] =  3
    LUT[27] =  1
    LUT[29] =  1
    LUT[31] = -1
    LUT[33] = -3
    LUT[35] = -1
    LUT[37] =  3
    LUT[39] =  1
    LUT[41] =  1
    LUT[43] = -1
    LUT[45] =  3
    LUT[47] =  1
    LUT[49] = -1
    LUT[51] =  1

    LUT[53] =  1
    LUT[55] = -1
    LUT[57] =  3
    LUT[59] =  1
    LUT[61] =  1
    LUT[63] = -1
    LUT[65] = -3
    LUT[67] =  3
    LUT[69] = -1
    LUT[71] =  1
    LUT[73] =  1
    LUT[75] =  3
    LUT[77] = -1
    LUT[79] =  1
    LUT[81] = -1
    LUT[83] =  1
    LUT[85] =  1
    LUT[87] = -1
    LUT[89] =  3
    LUT[91] =  1
    LUT[93] =  1
    LUT[95] = -1
    LUT[97] =  1
    LUT[99] =  3
    LUT[101] =  3
    LUT[103] =  1

    LUT[105] =  5
    LUT[107] =  3
    LUT[109] =  3
    LUT[111] =  1
    LUT[113] = -1
    LUT[115] =  1
    LUT[117] =  1
    LUT[119] = -1
    LUT[121] =  3
    LUT[123] =  1
    LUT[125] =  1
    LUT[127] = -1
    LUT[129] = -7
    LUT[131] = -1
    LUT[133] = -1
    LUT[135] =  1
    LUT[137] = -3
    LUT[139] = -1
    LUT[141] = -1
    LUT[143] =  1
    LUT[145] = -1
    LUT[147] =  1
    LUT[149] =  1
    LUT[151] = -1
    LUT[153] =  3
    LUT[155] =  1

    LUT[157] =  1
    LUT[159] = -1
    LUT[161] = -3
    LUT[163] = -1
    LUT[165] =  3
    LUT[167] =  1
    LUT[169] =  1
    LUT[171] = -1
    LUT[173] =  3
    LUT[175] =  1
    LUT[177] = -1
    LUT[179] =  1
    LUT[181] =  1
    LUT[183] = -1
    LUT[185] =  3
    LUT[187] =  1
    LUT[189] =  1
    LUT[191] = -1
    LUT[193] = -3
    LUT[195] =  3
    LUT[197] = -1
    LUT[199] =  1
    LUT[201] =  1
    LUT[203] =  3
    LUT[205] = -1
    LUT[207] =  1

    LUT[209] = -1
    LUT[211] =  1
    LUT[213] =  1
    LUT[215] = -1
    LUT[217] =  3
    LUT[219] =  1
    LUT[221] =  1
    LUT[223] = -1
    LUT[225] =  1
    LUT[227] =  3
    LUT[229] =  3
    LUT[231] =  1
    LUT[233] =  5
    LUT[235] =  3
    LUT[237] =  3
    LUT[239] =  1
    LUT[241] = -1
    LUT[243] =  1
    LUT[245] =  1
    LUT[247] = -1
    LUT[249] =  3
    LUT[251] =  1
    LUT[253] =  1
    LUT[255] = -1
    return LUT

LUT = fill_Euler_LUT()


### Octants (indexOctantXXX functions)
OCTANTS = tuple(range(8))
NEB, NWB, SEB, SWB, NEU, NWU, SEU, SWU = OCTANTS

neib_idx = np.empty((8, 7), dtype=int)
neib_idx[NEB, ...] = [2, 1, 11, 10, 5, 4, 14]
neib_idx[NWB, ...] = [0, 9, 3, 12, 1, 10, 4]
neib_idx[SEB, ...] = [8, 7, 17, 16, 5, 4, 14]
neib_idx[SWB, ...] = [6, 15, 7, 16, 3, 12, 4]
neib_idx[NEU, ...] = [20, 23, 19, 22, 11, 14, 10]
neib_idx[NWU, ...] = [18, 21, 9, 12, 19, 22, 10]
neib_idx[SEU, ...] = [26, 23, 17, 14, 25, 22, 16]
neib_idx[SWU, ...] = [24, 25, 15, 16, 21, 22, 12]

def index_octants(octant, neighbors):
    n = 1
    for j, idx in enumerate(neib_idx[octant]):
        if neighbors[idx] == 1:
            n |= 2**(7 - j)
    return n


def is_surfacepoint(neighbors, points_LUT):
    for octant in OCTANTS:
        n = index_octants(octabt, neighbors)
        if n not in (240, 165, 170) and points_LUT[n] > 2:
            return False
    return True


def is_Euler_invariant(neighbors):
    """Check if a point is Euler invariant.

    Calculate Euler characteristc for each octant and sum up.

    Parameters
    ----------
    neighbors : ndarray, shape (27,)
        neighbors of a point

    Returns
    -------
    bool

    """
    euler_char = 0
    for octant in OCTANTS:
        n = index_octants(octant, neighbors)
        euler_char += LUT[n]
    return euler_char == 0


def is_simple_point(neighbors):
    """Check is a point is a Simple Point.

    This method is named 'N(v)_labeling' in [Lee94].
    Outputs the number of connected objects in a neighborhood of a point
    after this point would have been removed.

    Parameters
    ----------
    neighbors : ndarray, shape(27,)
        neighbors of the point

    Returns
    -------
    bool
        Whether the point is simple or not.

    """
    # copy neighbors for labeling
    # ignore center pixel (i=13) when counting (see [Lee94])
    cube = np.r_[neighbors[:13], neighbors[14:]]

    # set initial label
    label = 2

    # for all point in the neighborhood
    for i in range(26):
        if cube[i] == 1:
            # voxel has not been labeled yet
            # start recursion with any octant that contains the point i
            if i in (0, 1, 3, 4, 9, 10, 12):
                octree_labeling(1, label, cube)
            elif i in (2, 5, 11, 13):
                octree_labeling(2, label, cube)
            elif i in (6, 7, 14, 15):
                octree_labeling(3, label, cube)
            elif i in (8, 16):
                octree_labeling(4, label, cube)
            elif i in (17, 18, 20, 21):
                octree_labeling(5, label, cube)
            elif i in (19, 22):
                octree_labeling(6, label, cube)
            elif i in (23, 24):
                octree_labeling(7, label, cube)
            elif i == 25:
                octree_labeling(8, label, cube)
            else:
                raise ValueError("Never be here. i = %s" % i)
            label += 1
            if label - 2 >= 2:
                return False
    return True


def octree_labeling(octant, label, cube):
    """This is a recursive method that calculates the number of connected
    components in the 3D neighborhood after the center pixel would
    have been removed.

    Parameters
    ----------
    octant : int
        octant index
    label : int 
        the current label of the center point
    cube : ndarray, shape(26,)
        local neighborhood of the point

    """
    # check if there are points in the octant with value 1
    if octant == 1:
        # set points in this octant to current label
        # and recursive labeling of adjacent octants
        if cube[0] == 1:
            cube[0] = label
        if cube[1] == 1:
            cube[1] = label
            octree_labeling(2, label, cube)
        if cube[3] == 1:
            cube[3] = label
            octree_labeling(3, label, cube)
        if cube[4] == 1:
            cube[4] = label
            octree_labeling(2, label, cube)
            octree_labeling(3, label, cube)
            octree_labeling(4, label, cube)
        if cube[9] == 1:
            cube[9] = label
            octree_labeling(5, label, cube)
        if cube[10] == 1:
            cube[10] = label
            octree_labeling(2, label, cube)
            octree_labeling(5, label, cube)
            octree_labeling(6, label, cube)
        if cube[12] == 1:
            cube[12] = label
            octree_labeling(3, label, cube)
            octree_labeling(5, label, cube)
            octree_labeling(7, label, cube)

    if octant == 2:
        if cube[1] == 1:
            cube[1] = label
            octree_labeling(1, label, cube)
        if cube[4] == 1:
              cube[4] = label
              octree_labeling(1, label, cube)
              octree_labeling(3, label, cube)
              octree_labeling(4, label, cube)
        if cube[10] == 1:
              cube[10] = label
              octree_labeling(1, label, cube)
              octree_labeling(5, label, cube)
              octree_labeling(6, label, cube)
        if cube[2] == 1:
              cube[2] = label
        if cube[5] == 1:
              cube[5] = label
              octree_labeling(4, label, cube)
        if cube[11] == 1:
              cube[11] = label
              octree_labeling(6, label, cube)
        if cube[13] == 1:
              cube[13] = label
              octree_labeling(4, label, cube)
              octree_labeling(6, label, cube)
              octree_labeling(8, label, cube)

    if octant ==3:
        if cube[3] == 1:
              cube[3] = label
              octree_labeling(1, label, cube)
        if cube[4] == 1:
              cube[4] = label
              octree_labeling(1, label, cube)
              octree_labeling(2, label, cube)
              octree_labeling(4, label, cube)
        if cube[12] == 1:
              cube[12] = label
              octree_labeling(1, label, cube)
              octree_labeling(5, label, cube)
              octree_labeling(7, label, cube)
        if cube[6] == 1:
              cube[6] = label
        if cube[7] == 1:
              cube[7] = label
              octree_labeling(4, label, cube)
        if cube[14] == 1:
              cube[14] = label
              octree_labeling(7, label, cube)
        if cube[15] == 1:
              cube[15] = label
              octree_labeling(4, label, cube)
              octree_labeling(7, label, cube)
              octree_labeling(8, label, cube)

    if octant == 4:
        if cube[4] == 1:
              cube[4] = label
              octree_labeling(1, label, cube)
              octree_labeling(2, label, cube)
              octree_labeling(3, label, cube)
        if cube[5] == 1:
              cube[5] = label
              octree_labeling(2, label, cube)
        if cube[13] == 1:
              cube[13] = label
              octree_labeling(2, label, cube)
              octree_labeling(6, label, cube)
              octree_labeling(8, label, cube)
        if cube[7] == 1:
              cube[7] = label
              octree_labeling(3, label, cube)
        if cube[15] == 1:
              cube[15] = label
              octree_labeling(3, label, cube)
              octree_labeling(7, label, cube)
              octree_labeling(8, label, cube)
        if cube[8] == 1:
              cube[8] = label
        if cube[16] == 1:
              cube[16] = label
              octree_labeling(8, label, cube)

    if octant == 5:
        if cube[9] == 1:
              cube[9] = label
              octree_labeling(1, label, cube)
        if cube[10] == 1:
              cube[10] = label
              octree_labeling(1, label, cube)
              octree_labeling(2, label, cube)
              octree_labeling(6, label, cube)
        if cube[12] == 1:
              cube[12] = label
              octree_labeling(1, label, cube)
              octree_labeling(3, label, cube)
              octree_labeling(7, label, cube)
        if cube[17] == 1:
              cube[17] = label
        if cube[18] == 1:
              cube[18] = label
              octree_labeling(6, label, cube)
        if cube[20] == 1:
              cube[20] = label
              octree_labeling(7, label, cube)
        if cube[21] == 1:
              cube[21] = label
              octree_labeling(6, label, cube)
              octree_labeling(7, label, cube)
              octree_labeling(8, label, cube)

    if octant == 6:
        if cube[10] == 1:
              cube[10] = label
              octree_labeling(1, label, cube)
              octree_labeling(2, label, cube)
              octree_labeling(5, label, cube)
        if cube[11] == 1:
              cube[11] = label
              octree_labeling(2, label, cube)
        if cube[13] == 1:
              cube[13] = label
              octree_labeling(2, label, cube)
              octree_labeling(4, label, cube)
              octree_labeling(8, label, cube)
        if cube[18] == 1:
              cube[18] = label
              octree_labeling(5, label, cube)
        if cube[21] == 1:
              cube[21] = label
              octree_labeling(5, label, cube)
              octree_labeling(7, label, cube)
              octree_labeling(8, label, cube)
        if cube[19] == 1:
              cube[19] = label
        if cube[22] == 1:
              cube[22] = label
              octree_labeling(8, label, cube)

    if octant == 7:
        if cube[12] == 1:
              cube[12] = label
              octree_labeling(1, label, cube)
              octree_labeling(3, label, cube)
              octree_labeling(5, label, cube)
        if cube[14] == 1:
              cube[14] = label
              octree_labeling(3, label, cube)
        if cube[15] == 1:
              cube[15] = label
              octree_labeling(3, label, cube)
              octree_labeling(4, label, cube)
              octree_labeling(8, label, cube)
        if cube[20] == 1:
              cube[20] = label
              octree_labeling(5, label, cube)
        if cube[21] == 1:
              cube[21] = label
              octree_labeling(5, label, cube)
              octree_labeling(6, label, cube)
              octree_labeling(8, label, cube)
        if cube[23] == 1:
              cube[23] = label
        if cube[24] == 1:
              cube[24] = label
              octree_labeling(8, label, cube)

    if octant == 8:
        if cube[13] == 1:
              cube[13] = label
              octree_labeling(2, label, cube)
              octree_labeling(4, label, cube)
              octree_labeling(6, label, cube)
        if cube[15] == 1:
              cube[15] = label
              octree_labeling(3, label, cube)
              octree_labeling(4, label, cube)
              octree_labeling(7, label, cube)
        if cube[16] == 1:
              cube[16] = label
              octree_labeling(4, label, cube)
        if cube[21] == 1:
              cube[21] = label
              octree_labeling(5, label, cube)
              octree_labeling(6, label, cube)
              octree_labeling(7, label, cube)
        if cube[22] == 1:
              cube[22] = label
              octree_labeling(6, label, cube)
        if cube[24] == 1:
              cube[24] = label
              octree_labeling(7, label, cube)
        if cube[25] == 1:
              cube[25] = label


def _loop_through(img, curr_border):
    """Inner loop of compute_thin_image.

    return simple_border_points as a list to be rechecked sequentially.
    """
    # loop through the image
    # NB: each loop is from 1 to size-1: img is padded from all sides 
    simple_border_points = []

    ### XXX: 2D images
    ### if the original is 2D, img.shape[0] == 3, the algorithm removes too much
    ### because all points are considered 'boundary' in the 3rd direction.
    ### Hence just bail out
    if img.shape[2] == 3 and curr_border in (5, 6):
        print("skipping curr_border = ", curr_border)
        return []

    for z in range(1, img.shape[2] - 1):
        for y in range(1, img.shape[1] - 1):
            for x in range(1, img.shape[0] - 1):

                # check if pixel is foreground
                if img[x, y, z] != 1:
                    continue

                is_border_pt = (curr_border == 1 and N(img, x, y, z) <= 0 or
                                curr_border == 2 and S(img, x, y, z) <= 0 or
                                curr_border == 3 and E(img, x, y, z) <= 0 or
                                curr_border == 4 and W(img, x, y, z) <= 0 or
                                curr_border == 5 and U(img, x, y, z) <= 0 or
                                curr_border == 6 and B(img, x, y, z) <= 0)
                if not is_border_pt:
                    # current point is not deletable
                    continue

                neighborhood = get_neighborhood(img, x, y, z)

                # check if (x, y, z) is an endpoint. An endpoint has exactly
                # one neighbor in the 26-neighborhood.
                # The center pixel is counted, thus r.h.s. is 2
                if neighborhood.sum() == 2:
                    continue

                # check if point is Euler invariant (condition 1 in [Lee94])
                # if it is not, it's not deletable
                if not is_Euler_invariant(neighborhood):
                    continue

                # check if point is simple (i.e., deletion does not
                # change connectivity in the 3x3x3 neighborhood)
                # this are conditions 2 and 3 in [Lee94]
                if not is_simple_point(neighborhood):
                    continue

                # ok, add (x, y, z) to the list of simple border points
                simple_border_points.append((x, y, z))
    return simple_border_points


def compute_thin_image(img_in):

    ### prepare
    img = prepare_image(img_in)

    ### compute

    # loop through the image several times until there is no change
    simple_border_points = []
    unchanged_borders = 0
    while unchanged_borders < 6:
        # loop until there is no change for all the six border types
        unchanged_borders = 0
        for curr_border in (4, 3, 2, 1, 5, 6):

            simple_border_points = _loop_through(img, curr_border)
            print(curr_border, " : ", simple_border_points, '\n')

            # sequential re-checking to preserve connectivity when deleting
            # in a parallel way
            no_change = True
            for pt in simple_border_points:
                neighb = get_neighborhood(img, *pt)
                if is_simple_point(neighb):
                    img[pt[0], pt[1], pt[2]] = 0
                    no_change = False
                else:
                    print(" *** ", pt, is_simple_point(neighb))

            if no_change:
                unchanged_borders += 1
            simple_border_points = []

    img = postprocess_image(img)
    return img


if __name__ == "__main__":
    pass
