package Skeletonize3D_;

/**
 * Skeletonize3D plugin for ImageJ(C).
 * Copyright (c) 2015 Ignacio Arganda-Carreras 
 * All rights reserved.
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. All advertising materials mentioning features or use of this software
 *    must display the following acknowledgement:
 *    This product includes software developed by the University of 
 *    California, Berkeley and its contributors.
 * 4. Neither the name of the University nor the names of its contributors
 *    may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY <COPYRIGHT HOLDER> ''AS IS'' AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * 
 */

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;

import java.util.ArrayList;

/**
 * Main class.
 * This class is a plugin for the ImageJ interface for 2D and 3D thinning 
 * (skeletonization) of binary images (2D/3D).
 *
 * <p>
 * This work is an implementation by Ignacio Arganda-Carreras of the
 * 3D thinning algorithm from Lee et al. "Building skeleton models via 3-D 
 * medial surface/axis thinning algorithms. Computer Vision, Graphics, and 
 * Image Processing, 56(6):462–478, 1994." Based on the ITK version from
 * Hanno Homann <a href="http://hdl.handle.net/1926/1292"> http://hdl.handle.net/1926/1292</a>
 * <p>
 *  More information at Skeletonize3D homepage:
 *  http://fiji.sc/Skeletonize3D
 *
 * @version 1.0 11/13/2015 (unique BSD licensed version for scikit-image)
 * @author Ignacio Arganda-Carreras (iargandacarreras at gmail.com)
 *
 */
public class Skeletonize3D_ implements PlugInFilter 
{
	/** working image plus */
	private ImagePlus imRef;

	/** working image width */
	private int width = 0;
	/** working image height */
	private int height = 0;
	/** working image depth */
	private int depth = 0;
	/** working image stack*/
	private ImageStack inputImage = null;
	
	/* -----------------------------------------------------------------------*/
	/**
	 * This method is called once when the filter is loaded.
	 * 
	 * @param arg argument specified for this plugin
	 * @param imp currently active image
	 * @return flag word that specifies the filters capabilities
	 */
	public int setup(String arg, ImagePlus imp) 
	{
		this.imRef = imp;
		
		if (arg.equals("about")) {
			showAbout();
			return DONE;
		}

		return DOES_8G;
	} /* end setup */
	
	/* -----------------------------------------------------------------------*/
	/**
	 * Process the image.
	 * 
	 * @see ij.plugin.filter.PlugInFilter#run(ij.process.ImageProcessor)
	 */
	public void run(ImageProcessor ip) 
	{
		
		this.width = this.imRef.getWidth();
		this.height = this.imRef.getHeight();
		this.depth = this.imRef.getStackSize();
		this.inputImage = this.imRef.getStack();
							
		// Prepare data
		prepareData(this.inputImage);
		
		// Compute Thinning	
		computeThinImage(this.inputImage);
		
		// Convert image to binary 0-255
		for(int i = 1; i <= this.inputImage.getSize(); i++)
			this.inputImage.getProcessor(i).multiply(255);
		
		this.inputImage.update(ip);
		

	} /* end run */

	/* -----------------------------------------------------------------------*/
	/**
	 * Prepare data for computation.
	 * Copy the input image to the output image, changing from the input
	 * type to the output type.
	 * 
	 * @param outputImage output image stack
	 */
	public void prepareData(ImageStack outputImage) 
	{
	
		//IJ.write("Prepare Data: Copy input to output");
		IJ.showStatus("Prepare Data: Copy input to output ...");
		
		// Copy the input to the output, changing all foreground pixels to
        // have value 1 in the process.
		for (int z = 0; z < depth; z++) 
			for (int x = 0; x < width; x++) 
				for (int y = 0; y < height; y++)
					if ( ((byte[]) this.inputImage.getPixels(z + 1))[x + y * width] != 0 )
						((byte[]) outputImage.getPixels(z + 1))[x + y * width] = 1;
				
		//IJ.write("Prepare Data End");
		IJ.showStatus("Prepare Data End.");
	} /* end prepareData */
	
	
	
	/* -----------------------------------------------------------------------*/
	/**
	 * Post processing for computing thinning.
	 * 
	 * @param outputImage output image stack
	 */
	public void computeThinImage(ImageStack outputImage) 
	{
		//IJ.write("Compute Thin Image Start");
		IJ.showStatus("Computing thin image ...");
						
		// Prepare Euler LUT [Lee94]
		int eulerLUT[] = new int[256]; 
		fillEulerLUT( eulerLUT );
		
		// Prepare number of points LUT
		int pointsLUT[] = new int[ 256 ];
		fillnumOfPointsLUT(pointsLUT);
		
		// Following Lee[94], save versions (Q) of input image S, while 
		// deleting each type of border points (R)
		//ImagePlus Q[] = new ImagePlus[6];
		ArrayList <int[]> simpleBorderPoints = new ArrayList<int[]>();				
		
		int iter = 1;
		//boolean debug = false;			
		// Loop through the image several times until there is no change.
		int unchangedBorders = 0;
		while( unchangedBorders < 6 )  // loop until no change for all the six border types
		{						
			unchangedBorders = 0;
			for( int currentBorder = 1; currentBorder <= 6; currentBorder++)
			{
				//Q[ currentBorder-1 ] = new ImagePlus("Q-" + currentBorder, outputImage).duplicate();
				
				IJ.showStatus("Thinning iteration " + iter + " (" + currentBorder +"/6 borders) ...");
				
				//if( debug ) IJ.log( " *** current border = " + currentBorder + " ***");	
				
				boolean noChange = true;				
				
				// Loop through the image.				 
				for (int z = 0; z < depth; z++)
				{
					for (int y = 0; y < height; y++)
					{
						for (int x = 0; x < width; x++)						
						{

							// check if point is foreground
							if ( getPixelNoCheck(outputImage, x, y, z) != 1 )
							{
								//IJ.log("  background");
								continue;         // current point is already background 
							}
																				
							// check 6-neighbors if point is a border point of type currentBorder
							boolean isBorderPoint = false;
							// North
							if( currentBorder == 1 && N(outputImage, x, y, z) <= 0 )
								isBorderPoint = true;
							// South
							if( currentBorder == 2 && S(outputImage, x, y, z) <= 0 )
								isBorderPoint = true;
							// East
							if( currentBorder == 3 && E(outputImage, x, y, z) <= 0 )
								isBorderPoint = true;
							// West
							if( currentBorder == 4 && W(outputImage, x, y, z) <= 0 )
								isBorderPoint = true;
							if(outputImage.getSize() > 1)
							{
								// Up							
								if( currentBorder == 5 && U(outputImage, x, y, z) <= 0 )
									isBorderPoint = true;
								// Bottom
								if( currentBorder == 6 && B(outputImage, x, y, z) <= 0 )
									isBorderPoint = true;
							}
							if( !isBorderPoint )
							{
								//if (debug) IJ.log("  not border");
								continue;         // current point is not deletable
							}
							
							//else // remove border point from Q
							//	setPixelNoCheck( Q[ currentBorder -1 ].getImageStack(), x, y, z, (byte)0);


							if( isEndPoint( outputImage, x, y, z))
							{
								//if (debug) IJ.log("checking point (" + x + ", " + y + ", " + z + ")");
								//if (debug) IJ.log("  end point");
								continue;
							}

							final byte[] neighborhood = getNeighborhood(outputImage, x, y, z);
							
							// Check if point is Euler invariant (condition 1 in Lee[94])
							if( !isEulerInvariant( neighborhood, eulerLUT ) )
							{
								//if (debug) IJ.log("checking point (" + x + ", " + y + ", " + z + ")");
								//if (debug) IJ.log("  not Euler invariant");								
								continue;         // current point is not deletable
							}

							// Check if point is simple (deletion does not change connectivity in the 3x3x3 neighborhood)
							// (conditions 2 and 3 in Lee[94])
							if( !isSimplePoint( neighborhood ) )
							{
								//if (debug) IJ.log("checking point (" + x + ", " + y + ", " + z + ")");
								//if (debug) IJ.log("  not simple point");								
								continue;         // current point is not deletable
							}



							// add all simple border points to a list for sequential re-checking
							int[] index = new int[3];
							index[0] = x;
							index[1] = y;
							index[2] = z;
							simpleBorderPoints.add(index);							

						}
					}					
					IJ.showProgress(z, this.depth);				
				}							


				// sequential re-checking to preserve connectivity when
				// deleting in a parallel way
				//				boolean noChange = true;
				int[] index = null;
				
				
				for(int i = 0; 	i < simpleBorderPoints.size() ; i++)
				{					
					index = simpleBorderPoints.get(i);

					// Check if border points is simple			        
					if( isSimplePoint( getNeighborhood(outputImage, index[0], index[1], index[2]) )	 )
					{
						// we can delete the current point
						setPixel( outputImage, index[0], index[1], index[2], (byte) 0);
						noChange = false;
					}


				}

				if( noChange )
					unchangedBorders++;


				simpleBorderPoints.clear();

			} // end currentBorder for loop

			// Progress bar iterations
			iter++;
		}

		//IJ.write("Compute Thin Image End");
		IJ.showStatus("Computed thin image.");
	} /* end computeThinImage */	
	
	
	/**
	 * Check if point is the end of an arc
	 * 
	 * @param image
	 * @param x
	 * @param y
	 * @param z
	 * @return
	 */
	boolean isEndPoint(ImageStack image, int x, int y, int z)
	{
		int numberOfNeighbors = -1;   // -1 and not 0 because the center pixel will be counted as well
        byte[] neighbor = getNeighborhood(image, x, y, z);
        for( int i = 0; i < 27; i++ ) // i =  0..26
        {					        	
          if( neighbor[i] == 1 )
            numberOfNeighbors++;
        }

        return  numberOfNeighbors == 1;        
	}
	
	/* -----------------------------------------------------------------------*/
	/**
	 * Get neighborhood of a pixel in a 3D image (0 border conditions) 
	 * 
	 * @param image 3D image (ImageStack)
	 * @param x x- coordinate
	 * @param y y- coordinate
	 * @param z z- coordinate (in image stacks the indexes start at 1)
	 * @return corresponding 27-pixels neighborhood (0 if out of image)
	 */
	public byte[] getNeighborhood(ImageStack image, int x, int y, int z)
	{
		byte[] neighborhood = new byte[27];
		
		neighborhood[ 0] = getPixel(image, x-1, y-1, z-1);
		neighborhood[ 1] = getPixel(image, x  , y-1, z-1);
		neighborhood[ 2] = getPixel(image, x+1, y-1, z-1);
		
		neighborhood[ 3] = getPixel(image, x-1, y,   z-1);
		neighborhood[ 4] = getPixel(image, x,   y,   z-1);
		neighborhood[ 5] = getPixel(image, x+1, y,   z-1);
		
		neighborhood[ 6] = getPixel(image, x-1, y+1, z-1);
		neighborhood[ 7] = getPixel(image, x,   y+1, z-1);
		neighborhood[ 8] = getPixel(image, x+1, y+1, z-1);
		
		neighborhood[ 9] = getPixel(image, x-1, y-1, z  );
		neighborhood[10] = getPixel(image, x,   y-1, z  );
		neighborhood[11] = getPixel(image, x+1, y-1, z  );
		
		neighborhood[12] = getPixel(image, x-1, y,   z  );
		neighborhood[13] = getPixel(image, x,   y,   z  );
		neighborhood[14] = getPixel(image, x+1, y,   z  );
		
		neighborhood[15] = getPixel(image, x-1, y+1, z  );
		neighborhood[16] = getPixel(image, x,   y+1, z  );
		neighborhood[17] = getPixel(image, x+1, y+1, z  );
		
		neighborhood[18] = getPixel(image, x-1, y-1, z+1);
		neighborhood[19] = getPixel(image, x,   y-1, z+1);
		neighborhood[20] = getPixel(image, x+1, y-1, z+1);
		
		neighborhood[21] = getPixel(image, x-1, y,   z+1);
		neighborhood[22] = getPixel(image, x,   y,   z+1);
		neighborhood[23] = getPixel(image, x+1, y,   z+1);
		
		neighborhood[24] = getPixel(image, x-1, y+1, z+1);
		neighborhood[25] = getPixel(image, x,   y+1, z+1);
		neighborhood[26] = getPixel(image, x+1, y+1, z+1);
		
		return neighborhood;
	} /* end getNeighborhood */
	
	/* -----------------------------------------------------------------------*/
	/**
	 * Get pixel in 3D image (0 border conditions) 
	 * 
	 * @param image 3D image
	 * @param x x- coordinate
	 * @param y y- coordinate
	 * @param z z- coordinate (in image stacks the indexes start at 1)
	 * @return corresponding pixel (0 if out of image)
	 */
	private byte getPixel(ImageStack image, int x, int y, int z)
	{
		if(x >= 0 && x < this.width && y >= 0 && y < this.height && z >= 0 && z < this.depth)
			return ((byte[]) image.getPixels(z + 1))[x + y * this.width];
		else return 0;
	} /* end getPixel */
	
	/* -----------------------------------------------------------------------*/
	/**
	 * Get pixel in 3D image (no border checking) 
	 * 
	 * @param image 3D image
	 * @param x x- coordinate
	 * @param y y- coordinate
	 * @param z z- coordinate (in image stacks the indexes start at 1)
	 * @return corresponding pixel
	 */
	private byte getPixelNoCheck(ImageStack image, int x, int y, int z)
	{		
		return ((byte[]) image.getPixels(z + 1))[x + y * this.width];		
	} /* end getPixelNocheck */
	
	
	/* -----------------------------------------------------------------------*/
	/**
	 * Set pixel in 3D image 
	 * 
	 * @param image 3D image
	 * @param x x- coordinate
	 * @param y y- coordinate
	 * @param z z- coordinate (in image stacks the indexes start at 1)
	 * @param value pixel value
	 */
	private void setPixel(ImageStack image, int x, int y, int z, byte value)
	{
		if(x >= 0 && x < this.width && y >= 0 && y < this.height && z >= 0 && z < this.depth)
			((byte[]) image.getPixels(z + 1))[x + y * this.width] = value;
	} /* end setPixel */
	
	
	/* -----------------------------------------------------------------------*/
	/**
	 * Set pixel in 3D image (without checking border conditions)
	 * 
	 * @param image 3D image
	 * @param x x- coordinate
	 * @param y y- coordinate
	 * @param z z- coordinate (in image stacks the indexes start at 1)
	 * @param value pixel value
	 */
	private void setPixelNoCheck(ImageStack image, int x, int y, int z, byte value)
	{		
			((byte[]) image.getPixels(z + 1))[x + y * this.width] = value;
	} /* end setPixelNoCheck */
	
	/* -----------------------------------------------------------------------*/
	/**
	 * North neighborhood (0 border conditions) 
	 * 
	 * @param image 3D image
	 * @param x x- coordinate
	 * @param y y- coordinate
	 * @param z z- coordinate (in image stacks the indexes start at 1)
	 * @return corresponding north pixel
	 */
	private byte N(ImageStack image, int x, int y, int z)
	{
		return getPixel(image, x, y-1, z);
	} /* end N */
	
	/* -----------------------------------------------------------------------*/
	/**
	 * South neighborhood (0 border conditions) 
	 * 
	 * @param image 3D image
	 * @param x x- coordinate
	 * @param y y- coordinate
	 * @param z z- coordinate (in image stacks the indexes start at 1)
	 * @return corresponding south pixel
	 */
	private byte S(ImageStack image, int x, int y, int z)
	{
		return getPixel(image, x, y+1, z);
	} /* end S */
	
	/* -----------------------------------------------------------------------*/
	/**
	 * East neighborhood (0 border conditions) 
	 * 
	 * @param image 3D image
	 * @param x x- coordinate
	 * @param y y- coordinate
	 * @param z z- coordinate (in image stacks the indexes start at 1)
	 * @return corresponding east pixel
	 */
	private byte E(ImageStack image, int x, int y, int z)
	{
		return getPixel(image, x+1, y, z);
	} /* end E */
	
	/* -----------------------------------------------------------------------*/
	/**
	 * West neighborhood (0 border conditions) 
	 * 
	 * @param image 3D image
	 * @param x x- coordinate
	 * @param y y- coordinate
	 * @param z z- coordinate (in image stacks the indexes start at 1)
	 * @return corresponding west pixel
	 */
	private byte W(ImageStack image, int x, int y, int z)
	{
		return getPixel(image, x-1, y, z);
	} /* end W */
	
	/* -----------------------------------------------------------------------*/
	/**
	 * Up neighborhood (0 border conditions) 
	 * 
	 * @param image 3D image
	 * @param x x- coordinate
	 * @param y y- coordinate
	 * @param z z- coordinate (in image stacks the indexes start at 1)
	 * @return corresponding up pixel
	 */
	private byte U(ImageStack image, int x, int y, int z)
	{
		return getPixel(image, x, y, z+1);
	} /* end U */
	
	/* -----------------------------------------------------------------------*/
	/**
	 * Bottom neighborhood (0 border conditions) 
	 * 
	 * @param image 3D image
	 * @param x x- coordinate
	 * @param y y- coordinate
	 * @param z z- coordinate (in image stacks the indexes start at 1)
	 * @return corresponding bottom pixel
	 */
	private byte B(ImageStack image, int x, int y, int z)
	{
		return getPixel(image, x, y, z-1);
	} /* end B */
	
	/* -----------------------------------------------------------------------*/
	/**
	 * Fill Euler LUT
	 * 
	 * @param LUT Euler LUT
	 */
	private void fillEulerLUT(int[] LUT) 
	{
		LUT[1]  =  1;
		LUT[3]  = -1;
		LUT[5]  = -1;
		LUT[7]  =  1;
		LUT[9]  = -3;
		LUT[11] = -1;
		LUT[13] = -1;
		LUT[15] =  1;
		LUT[17] = -1;
		LUT[19] =  1;
		LUT[21] =  1;
		LUT[23] = -1;
		LUT[25] =  3;
		LUT[27] =  1;
		LUT[29] =  1;
		LUT[31] = -1;
		LUT[33] = -3;
		LUT[35] = -1;
		LUT[37] =  3;
		LUT[39] =  1;
		LUT[41] =  1;
		LUT[43] = -1;
		LUT[45] =  3;
		LUT[47] =  1;
		LUT[49] = -1;
		LUT[51] =  1;

		LUT[53] =  1;
		LUT[55] = -1;
		LUT[57] =  3;
		LUT[59] =  1;
		LUT[61] =  1;
		LUT[63] = -1;
		LUT[65] = -3;
		LUT[67] =  3;
		LUT[69] = -1;
		LUT[71] =  1;
		LUT[73] =  1;
		LUT[75] =  3;
		LUT[77] = -1;
		LUT[79] =  1;
		LUT[81] = -1;
		LUT[83] =  1;
		LUT[85] =  1;
		LUT[87] = -1;
		LUT[89] =  3;
		LUT[91] =  1;
		LUT[93] =  1;
		LUT[95] = -1;
		LUT[97] =  1;
		LUT[99] =  3;
		LUT[101] =  3;
		LUT[103] =  1;

		LUT[105] =  5;
		LUT[107] =  3;
		LUT[109] =  3;
		LUT[111] =  1;
		LUT[113] = -1;
		LUT[115] =  1;
		LUT[117] =  1;
		LUT[119] = -1;
		LUT[121] =  3;
		LUT[123] =  1;
		LUT[125] =  1;
		LUT[127] = -1;
		LUT[129] = -7;
		LUT[131] = -1;
		LUT[133] = -1;
		LUT[135] =  1;
		LUT[137] = -3;
		LUT[139] = -1;
		LUT[141] = -1;
		LUT[143] =  1;
		LUT[145] = -1;
		LUT[147] =  1;
		LUT[149] =  1;
		LUT[151] = -1;
		LUT[153] =  3;
		LUT[155] =  1;

		LUT[157] =  1;
		LUT[159] = -1;
		LUT[161] = -3;
		LUT[163] = -1;
		LUT[165] =  3;
		LUT[167] =  1;
		LUT[169] =  1;
		LUT[171] = -1;
		LUT[173] =  3;
		LUT[175] =  1;
		LUT[177] = -1;
		LUT[179] =  1;
		LUT[181] =  1;
		LUT[183] = -1;
		LUT[185] =  3;
		LUT[187] =  1;
		LUT[189] =  1;
		LUT[191] = -1;
		LUT[193] = -3;
		LUT[195] =  3;
		LUT[197] = -1;
		LUT[199] =  1;
		LUT[201] =  1;
		LUT[203] =  3;
		LUT[205] = -1;
		LUT[207] =  1;

		LUT[209] = -1;
		LUT[211] =  1;
		LUT[213] =  1;
		LUT[215] = -1;
		LUT[217] =  3;
		LUT[219] =  1;
		LUT[221] =  1;
		LUT[223] = -1;
		LUT[225] =  1;
		LUT[227] =  3;
		LUT[229] =  3;
		LUT[231] =  1;
		LUT[233] =  5;
		LUT[235] =  3;
		LUT[237] =  3;
		LUT[239] =  1;
		LUT[241] = -1;
		LUT[243] =  1;
		LUT[245] =  1;
		LUT[247] = -1;
		LUT[249] =  3;
		LUT[251] =  1;
		LUT[253] =  1;
		LUT[255] = -1;
	}
	
	
	/* -----------------------------------------------------------------------*/
	/**
	 * Fill number of points in octant LUT
	 * 
	 * @param LUT number of points in octant LUT
	 */
	public void fillnumOfPointsLUT(int[] LUT) 
	{
		for(int i=0; i<256; i++)
			LUT[ i ] = Integer.bitCount( i );			
	}

	/**
	 * Check if a point is Euler invariant
	 * 
	 * @param neighbors neighbor pixels of the point
	 * @param LUT Euler LUT
	 * @return true or false if the point is Euler invariant or not
	 */
	boolean isEulerInvariant(byte[] neighbors, int [] LUT)
	{
		// Calculate Euler characteristic for each octant and sum up
		int eulerChar = 0;
		char n;
		// Octant SWU
		n = indexOctantSWU(neighbors);
		eulerChar += LUT[n];
		
		// Octant SEU
		n = indexOctantSEU(neighbors);
		eulerChar += LUT[n];
		
		// Octant NWU
		n = indexOctantNWU(neighbors);
		eulerChar += LUT[n];
		
		// Octant NEU
		n = indexOctantNEU(neighbors);
		eulerChar += LUT[n];
		
		// Octant SWB
		n = indexOctantSWB(neighbors);
		eulerChar += LUT[n];
		
		// Octant SEB
		n = indextOctantSEB(neighbors);
		eulerChar += LUT[n];
		
		// Octant NWB
		n = indexOctantNWB(neighbors);
		eulerChar += LUT[n];
		
		// Octant NEB
		n = indexOctantNEB(neighbors);
		eulerChar += LUT[n];
		
		if( eulerChar == 0 )
			return true;
		else
			return false;
		}

	public char indexOctantNEB(byte[] neighbors) {
		char n;
		n = 1;
		if( neighbors[2]==1 )
			n |= 128;
		if( neighbors[1]==1 )
			n |=  64;
		if( neighbors[11]==1 )
			n |=  32;
		if( neighbors[10]==1 )
			n |=  16;
		if( neighbors[5]==1 )
			n |=   8;
		if( neighbors[4]==1 )
			n |=   4;
		if( neighbors[14]==1 )
			n |=   2;
		return n;
	}

	public char indexOctantNWB(byte[] neighbors) {
		char n;
		n = 1;
		if( neighbors[0]==1 )
			n |= 128;
		if( neighbors[9]==1 )
			n |=  64;
		if( neighbors[3]==1 )
			n |=  32;
		if( neighbors[12]==1 )
			n |=  16;
		if( neighbors[1]==1 )
			n |=   8;
		if( neighbors[10]==1 )
			n |=   4;
		if( neighbors[4]==1 )
			n |=   2;
		return n;
	}

	public char indextOctantSEB(byte[] neighbors) {
		char n;
		n = 1;
		if( neighbors[8]==1 )
			n |= 128;
		if( neighbors[7]==1 )
			n |=  64;
		if( neighbors[17]==1 )
			n |=  32;
		if( neighbors[16]==1 )
			n |=  16;
		if( neighbors[5]==1 )
			n |=   8;
		if( neighbors[4]==1 )
			n |=   4;
		if( neighbors[14]==1 )
			n |=   2;
		return n;
	}

	public char indexOctantSWB(byte[] neighbors) {
		char n;
		n = 1;
		if( neighbors[6]==1 )
			n |= 128;
		if( neighbors[15]==1 )
			n |=  64;
		if( neighbors[7]==1 )
			n |=  32;
		if( neighbors[16]==1 )
			n |=  16;
		if( neighbors[3]==1 )
			n |=   8;
		if( neighbors[12]==1 )
			n |=   4;
		if( neighbors[4]==1 )
			n |=   2;
		return n;
	}

	public char indexOctantNEU(byte[] neighbors) {
		char n;
		n = 1;
		if( neighbors[20]==1 )
			n |= 128;
		if( neighbors[23]==1 )
			n |=  64;
		if( neighbors[19]==1 )
			n |=  32;
		if( neighbors[22]==1 )
			n |=  16;
		if( neighbors[11]==1 )
			n |=   8;
		if( neighbors[14]==1 )
			n |=   4;
		if( neighbors[10]==1 )
			n |=   2;
		return n;
	}

	public char indexOctantNWU(byte[] neighbors) {
		char n;
		n = 1;
		if( neighbors[18]==1 )
			n |= 128;
		if( neighbors[21]==1 )
			n |=  64;
		if( neighbors[9]==1 )
			n |=  32;
		if( neighbors[12]==1 )
			n |=  16;
		if( neighbors[19]==1 )
			n |=   8;
		if( neighbors[22]==1 )
			n |=   4;
		if( neighbors[10]==1 )
			n |=   2;
		return n;
	}

	public char indexOctantSEU(byte[] neighbors) {
		char n;
		n = 1;
		if( neighbors[26]==1 )
			n |= 128;
		if( neighbors[23]==1 )
			n |=  64;
		if( neighbors[17]==1 )
			n |=  32;
		if( neighbors[14]==1 )
			n |=  16;
		if( neighbors[25]==1 )
			n |=   8;
		if( neighbors[22]==1 )
			n |=   4;
		if( neighbors[16]==1 )
			n |=   2;
		return n;
	}

	public char indexOctantSWU(byte[] neighbors) {
		char n;
		n = 1;
		if( neighbors[24]==1 )
			n |= 128;
		if( neighbors[25]==1 )
			n |=  64;
		if( neighbors[15]==1 )
			n |=  32;
		if( neighbors[16]==1 )
			n |=  16;
		if( neighbors[21]==1 )
			n |=   8;
		if( neighbors[22]==1 )
			n |=   4;
		if( neighbors[12]==1 )
			n |=   2;
		return n;
	}
	
	
	public boolean isSurfacePoint( byte[] neighbors, int[] pointsLUT )
	{		
		char n;
		// Octant SWU
		n = indexOctantSWU(neighbors);
		if( n != 240 && n != 165 && n != 170 && n != 204 && (pointsLUT[ n ] > 2) )
			return false;
		
		// Octant SEU
		n = indexOctantSEU(neighbors);
		if( n != 240 && n != 165 && n != 170 && n != 204 && (pointsLUT[ n ] > 2) )
			return false;
		
		// Octant NWU
		n = indexOctantNWU(neighbors);
		if( n != 240 && n != 165 && n != 170 && n != 204 && (pointsLUT[ n ] > 2) )
			return false;
		
		// Octant NEU
		n = indexOctantNEU(neighbors);
		if( n != 240 && n != 165 && n != 170 && n != 204 && (pointsLUT[ n ] > 2) )
			return false;
		
		// Octant SWB
		n = indexOctantSWB(neighbors);
		if( n != 240 && n != 165 && n != 170 && n != 204 && (pointsLUT[ n ] > 2) )
			return false;
		
		// Octant SEB
		n = indextOctantSEB(neighbors);
		if( n != 240 && n != 165 && n != 170 && n != 204 && (pointsLUT[ n ] > 2) )
			return false;
		
		// Octant NWB
		n = indexOctantNWB(neighbors);
		if( n != 240 && n != 165 && n != 170 && n != 204 && (pointsLUT[ n ] > 2) )
			return false;
		
		// Octant NEB
		n = indexOctantNEB(neighbors);
		if( n != 240 && n != 165 && n != 170 && n != 204 && (pointsLUT[ n ] > 2) )
			return false;
		
		return true;
	}
	
	/* -----------------------------------------------------------------------*/
	/**
	 * Check if current point is a Simple Point.
	 * This method is named 'N(v)_labeling' in [Lee94].
	 * Outputs the number of connected objects in a neighborhood of a point
	 * after this point would have been removed.
	 * 
	 * @param neighbors neighbor pixels of the point
	 * @return true or false if the point is simple or not
	 */
	private boolean isSimplePoint(byte[] neighbors) 
	{
		// copy neighbors for labeling
		int cube[] = new int[26];
		int i = 0;
		for( i = 0; i < 13; i++ )  // i =  0..12 -> cube[0..12]
			cube[i] = neighbors[i];
		// i != 13 : ignore center pixel when counting (see [Lee94])
		for( i = 14; i < 27; i++ ) // i = 14..26 -> cube[13..25]
			cube[i-1] = neighbors[i];
		// set initial label
		int label = 2;
		// for all points in the neighborhood
		for( i = 0; i < 26; i++ )
		{
			if( cube[i]==1 )     // voxel has not been labeled yet
			{
				// start recursion with any octant that contains the point i
				switch( i )
				{
				case 0:
				case 1:
				case 3:
				case 4:
				case 9:
				case 10:
				case 12:
					octreeLabeling(1, label, cube );
					break;
				case 2:
				case 5:
				case 11:
				case 13:
					octreeLabeling(2, label, cube );
					break;
				case 6:
				case 7:
				case 14:
				case 15:
					octreeLabeling(3, label, cube );
					break;
				case 8:
				case 16:
					octreeLabeling(4, label, cube );
					break;
				case 17:
				case 18:
				case 20:
				case 21:
					octreeLabeling(5, label, cube );
					break;
				case 19:
				case 22:
					octreeLabeling(6, label, cube );
					break;
				case 23:
				case 24:
					octreeLabeling(7, label, cube );
					break;
				case 25:
					octreeLabeling(8, label, cube );
					break;
				}
				label++;
				if( label-2 >= 2 )
				{
					return false;
				}
			}
		}
		//return label-2; in [Lee94] if the number of connected components would be needed
		return true;
	}
	/* -----------------------------------------------------------------------*/
	/**
	 * This is a recursive method that calculates the number of connected
	 * components in the 3D neighborhood after the center pixel would
	 * have been removed.
	 * 
	 * @param octant
	 * @param label
	 * @param cube
	 */
	private void octreeLabeling(int octant, int label, int[] cube) 
	{
		// check if there are points in the octant with value 1
		  if( octant==1 )
		  {
		  	// set points in this octant to current label
		  	// and recursive labeling of adjacent octants
		    if( cube[0] == 1 )
		      cube[0] = label;
		    if( cube[1] == 1 )
		    {
		      cube[1] = label;        
		      octreeLabeling( 2, label, cube);
		    }
		    if( cube[3] == 1 )
		    {
		      cube[3] = label;        
		      octreeLabeling( 3, label, cube);
		    }
		    if( cube[4] == 1 )
		    {
		      cube[4] = label;        
		      octreeLabeling( 2, label, cube);
		      octreeLabeling( 3, label, cube);
		      octreeLabeling( 4, label, cube);
		    }
		    if( cube[9] == 1 )
		    {
		      cube[9] = label;        
		      octreeLabeling( 5, label, cube);
		    }
		    if( cube[10] == 1 )
		    {
		      cube[10] = label;        
		      octreeLabeling( 2, label, cube);
		      octreeLabeling( 5, label, cube);
		      octreeLabeling( 6, label, cube);
		    }
		    if( cube[12] == 1 )
		    {
		      cube[12] = label;        
		      octreeLabeling( 3, label, cube);
		      octreeLabeling( 5, label, cube);
		      octreeLabeling( 7, label, cube);
		    }
		  }
		  if( octant==2 )
		  {
		    if( cube[1] == 1 )
		    {
		      cube[1] = label;
		      octreeLabeling( 1, label, cube);
		    }
		    if( cube[4] == 1 )
		    {
		      cube[4] = label;        
		      octreeLabeling( 1, label, cube);
		      octreeLabeling( 3, label, cube);
		      octreeLabeling( 4, label, cube);
		    }
		    if( cube[10] == 1 )
		    {
		      cube[10] = label;        
		      octreeLabeling( 1, label, cube);
		      octreeLabeling( 5, label, cube);
		      octreeLabeling( 6, label, cube);
		    }
		    if( cube[2] == 1 )
		      cube[2] = label;        
		    if( cube[5] == 1 )
		    {
		      cube[5] = label;        
		      octreeLabeling( 4, label, cube);
		    }
		    if( cube[11] == 1 )
		    {
		      cube[11] = label;        
		      octreeLabeling( 6, label, cube);
		    }
		    if( cube[13] == 1 )
		    {
		      cube[13] = label;        
		      octreeLabeling( 4, label, cube);
		      octreeLabeling( 6, label, cube);
		      octreeLabeling( 8, label, cube);
		    }
		  }
		  if( octant==3 )
		  {
		    if( cube[3] == 1 )
		    {
		      cube[3] = label;        
		      octreeLabeling( 1, label, cube);
		    }
		    if( cube[4] == 1 )
		    {
		      cube[4] = label;        
		      octreeLabeling( 1, label, cube);
		      octreeLabeling( 2, label, cube);
		      octreeLabeling( 4, label, cube);
		    }
		    if( cube[12] == 1 )
		    {
		      cube[12] = label;        
		      octreeLabeling( 1, label, cube);
		      octreeLabeling( 5, label, cube);
		      octreeLabeling( 7, label, cube);
		    }
		    if( cube[6] == 1 )
		      cube[6] = label;        
		    if( cube[7] == 1 )
		    {
		      cube[7] = label;        
		      octreeLabeling( 4, label, cube);
		    }
		    if( cube[14] == 1 )
		    {
		      cube[14] = label;        
		      octreeLabeling( 7, label, cube);
		    }
		    if( cube[15] == 1 )
		    {
		      cube[15] = label;        
		      octreeLabeling( 4, label, cube);
		      octreeLabeling( 7, label, cube);
		      octreeLabeling( 8, label, cube);
		    }
		  }
		  if( octant==4 )
		  {
		  	if( cube[4] == 1 )
		    {
		      cube[4] = label;        
		      octreeLabeling( 1, label, cube);
		      octreeLabeling( 2, label, cube);
		      octreeLabeling( 3, label, cube);
		    }
		  	if( cube[5] == 1 )
		    {
		      cube[5] = label;        
		      octreeLabeling( 2, label, cube);
		    }
		    if( cube[13] == 1 )
		    {
		      cube[13] = label;        
		      octreeLabeling( 2, label, cube);
		      octreeLabeling( 6, label, cube);
		      octreeLabeling( 8, label, cube);
		    }
		    if( cube[7] == 1 )
		    {
		      cube[7] = label;        
		      octreeLabeling( 3, label, cube);
		    }
		    if( cube[15] == 1 )
		    {
		      cube[15] = label;        
		      octreeLabeling( 3, label, cube);
		      octreeLabeling( 7, label, cube);
		      octreeLabeling( 8, label, cube);
		    }
		    if( cube[8] == 1 )
		      cube[8] = label;        
		    if( cube[16] == 1 )
		    {
		      cube[16] = label;        
		      octreeLabeling( 8, label, cube);
		    }
		  }
		  if( octant==5 )
		  {
		  	if( cube[9] == 1 )
		    {
		      cube[9] = label;        
		      octreeLabeling( 1, label, cube);
		    }
		    if( cube[10] == 1 )
		    {
		      cube[10] = label;        
		      octreeLabeling( 1, label, cube);
		      octreeLabeling( 2, label, cube);
		      octreeLabeling( 6, label, cube);
		    }
		    if( cube[12] == 1 )
		    {
		      cube[12] = label;        
		      octreeLabeling( 1, label, cube);
		      octreeLabeling( 3, label, cube);
		      octreeLabeling( 7, label, cube);
		    }
		    if( cube[17] == 1 )
		      cube[17] = label;        
		    if( cube[18] == 1 )
		    {
		      cube[18] = label;        
		      octreeLabeling( 6, label, cube);
		    }
		    if( cube[20] == 1 )
		    {
		      cube[20] = label;        
		      octreeLabeling( 7, label, cube);
		    }
		    if( cube[21] == 1 )
		    {
		      cube[21] = label;        
		      octreeLabeling( 6, label, cube);
		      octreeLabeling( 7, label, cube);
		      octreeLabeling( 8, label, cube);
		    }
		  }
		  if( octant==6 )
		  {
		  	if( cube[10] == 1 )
		    {
		      cube[10] = label;        
		      octreeLabeling( 1, label, cube);
		      octreeLabeling( 2, label, cube);
		      octreeLabeling( 5, label, cube);
		    }
		    if( cube[11] == 1 )
		    {
		      cube[11] = label;        
		      octreeLabeling( 2, label, cube);
		    }
		    if( cube[13] == 1 )
		    {
		      cube[13] = label;        
		      octreeLabeling( 2, label, cube);
		      octreeLabeling( 4, label, cube);
		      octreeLabeling( 8, label, cube);
		    }
		    if( cube[18] == 1 )
		    {
		      cube[18] = label;        
		      octreeLabeling( 5, label, cube);
		    }
		    if( cube[21] == 1 )
		    {
		      cube[21] = label;        
		      octreeLabeling( 5, label, cube);
		      octreeLabeling( 7, label, cube);
		      octreeLabeling( 8, label, cube);
		    }
		    if( cube[19] == 1 )
		      cube[19] = label;        
		    if( cube[22] == 1 )
		    {
		      cube[22] = label;        
		      octreeLabeling( 8, label, cube);
		    }
		  }
		  if( octant==7 )
		  {
		  	if( cube[12] == 1 )
		    {
		      cube[12] = label;        
		      octreeLabeling( 1, label, cube);
		      octreeLabeling( 3, label, cube);
		      octreeLabeling( 5, label, cube);
		    }
		  	if( cube[14] == 1 )
		    {
		      cube[14] = label;        
		      octreeLabeling( 3, label, cube);
		    }
		    if( cube[15] == 1 )
		    {
		      cube[15] = label;        
		      octreeLabeling( 3, label, cube);
		      octreeLabeling( 4, label, cube);
		      octreeLabeling( 8, label, cube);
		    }
		    if( cube[20] == 1 )
		    {
		      cube[20] = label;        
		      octreeLabeling( 5, label, cube);
		    }
		    if( cube[21] == 1 )
		    {
		      cube[21] = label;        
		      octreeLabeling( 5, label, cube);
		      octreeLabeling( 6, label, cube);
		      octreeLabeling( 8, label, cube);
		    }
		    if( cube[23] == 1 )
		      cube[23] = label;        
		    if( cube[24] == 1 )
		    {
		      cube[24] = label;        
		      octreeLabeling( 8, label, cube);
		    }
		  }
		  if( octant==8 )
		  {
		  	if( cube[13] == 1 )
		    {
		      cube[13] = label;        
		      octreeLabeling( 2, label, cube);
		      octreeLabeling( 4, label, cube);
		      octreeLabeling( 6, label, cube);
		    }
		  	if( cube[15] == 1 )
		    {
		      cube[15] = label;        
		      octreeLabeling( 3, label, cube);
		      octreeLabeling( 4, label, cube);
		      octreeLabeling( 7, label, cube);
		    }
		  	if( cube[16] == 1 )
		    {
		      cube[16] = label;        
		      octreeLabeling( 4, label, cube);
		    }
		  	if( cube[21] == 1 )
		    {
		      cube[21] = label;        
		      octreeLabeling( 5, label, cube);
		      octreeLabeling( 6, label, cube);
		      octreeLabeling( 7, label, cube);
		    }
		  	if( cube[22] == 1 )
		    {
		      cube[22] = label;        
		      octreeLabeling( 6, label, cube);
		    }
		  	if( cube[24] == 1 )
		    {
		      cube[24] = label;        
		      octreeLabeling( 7, label, cube);
		    }
		  	if( cube[25] == 1 )
		      cube[25] = label;        
		  }
		
	}

	/* -----------------------------------------------------------------------*/
	/**
	 * Show plug-in information.
	 * 
	 */
	void showAbout() 
	{
		IJ.showMessage(
						"About Skeletonize3D...",
						"This plug-in filter produces 3D thinning (skeletonization) of binary 3D images.\n");
	} /* end showAbout */
	/* -----------------------------------------------------------------------*/

} /* end Skeletonize3D_ */
