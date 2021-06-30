package seamcarving

import java.awt.Color
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

class ImageHandler(val fileName: String, val outputName: String) {

    private var storedImage: BufferedImage
    private var maxEnergy: Double = Double.MIN_VALUE
    private var energies: MutableList<Double>

    init {
        val file = File(fileName)
        if (!file.extension.endsWith("png")) {
            println(fileName)
            throw RuntimeException("Not png format")
        }
        storedImage = ImageIO.read(File(fileName))
        energies = mutableListOf()
    }

    fun carveOutImage() {
        calculateEnergies()
        for (i in 0 until storedImage.width) {
            for (j in 0 until storedImage.height) {
                val intensity = (255.0 * energies.get(i * storedImage.height + j) / maxEnergy).toInt()
                val newColor = Color(intensity, intensity, intensity)
                storedImage.setRGB(i, j, newColor.rgb)
            }
        }
    }

    fun compress(numberWidth: Int, numberHeight: Int) {
        for (i in 1..numberWidth) {
            deleteMinSeam(false)
        }
        for (i in 1..numberHeight) {
            deleteMinSeam(true)
        }
    }

    private fun deleteMinSeam(horizontal: Boolean = false) {
        if (horizontal)
            transpose()

        calculateEnergies()
        // stores the neighbor in the row above. DP solution
        val pathMatrix = IntArray(storedImage.width * storedImage.height) { Integer.MIN_VALUE }
        val minSeamValue = DoubleArray(storedImage.width * storedImage.height) { Double.MAX_VALUE }

        // initialize first row
        var i = 0
        while (i < storedImage.width) {
            minSeamValue[i] = energies[i]
            i++
        }

        // iteratively calculate minimal seam energy
        i = 1
        while (i < storedImage.height) {
            var j = 0
            while (j < storedImage.width) {
                val matrixIndex = i * storedImage.width + j
                val leftAbove = (i - 1) * storedImage.width + j - 1
                val middleAbove = (i - 1) * storedImage.width + j
                val rightAbove = (i - 1) * storedImage.width + j + 1
                when (j) {
                    // left border case
                    0 -> {
                            if (minSeamValue[middleAbove] > minSeamValue[rightAbove]) {
                                    minSeamValue[matrixIndex] = minSeamValue[rightAbove] + energies[matrixIndex]
                                    pathMatrix[matrixIndex] = 1
                            } else {
                                minSeamValue[matrixIndex] = minSeamValue[middleAbove] + energies[matrixIndex]
                                pathMatrix[matrixIndex] = 0
                            }
                    }
                    // right border case
                    storedImage.width - 1 -> {
                        if (minSeamValue[middleAbove] > minSeamValue[leftAbove]) {
                            minSeamValue[matrixIndex] = minSeamValue[leftAbove] + energies[matrixIndex]
                            pathMatrix[matrixIndex] = -1
                        } else {
                            minSeamValue[matrixIndex] = minSeamValue[middleAbove] + energies[matrixIndex]
                            pathMatrix[matrixIndex] = 0
                        }
                    }
                    // default
                    else -> {
                        when {
                            minSeamValue[middleAbove] <= minSeamValue[leftAbove]
                                    && minSeamValue[middleAbove] <= minSeamValue[rightAbove] -> {
                                        minSeamValue[matrixIndex] = minSeamValue[middleAbove] + energies[matrixIndex]
                                        pathMatrix[matrixIndex] = 0
                                    }
                            minSeamValue[leftAbove] <= minSeamValue[middleAbove]
                                    && minSeamValue[leftAbove] <= minSeamValue[rightAbove] -> {
                                        minSeamValue[matrixIndex] = minSeamValue[leftAbove] + energies[matrixIndex]
                                        pathMatrix[matrixIndex] = -1
                                    }
                            minSeamValue[rightAbove] <= minSeamValue[middleAbove]
                                    && minSeamValue[rightAbove] <= minSeamValue[leftAbove] -> {
                                        minSeamValue[matrixIndex] = minSeamValue[rightAbove] + energies[matrixIndex]
                                        pathMatrix[matrixIndex] = 1
                                    }
                            else -> {
                                throw error("No suitable case found")
                            }
                        }
                    }
                }
                j++
            }
            i++
        }

        // traverse min seam and color it red
        // find start index
        var startIndex = 0
        var minValue = Double.MAX_VALUE
        i = 0
        while (i < storedImage.width) {
            if (minSeamValue[(storedImage.height - 1) * storedImage.width + i] < minValue) {
                minValue = minSeamValue[(storedImage.height - 1) * storedImage.width + i];
                startIndex = i;
            }
            i++
        }

        // create compressed image
        val compresedImage: BufferedImage =
            BufferedImage(storedImage.width - 1, storedImage.height, storedImage.type)

        i = storedImage.height - 1
        while (i >= 0) {
            // traverse width and set pixels
            var j = 0
            while (j < storedImage.width) {
                if (j < startIndex) {
                    compresedImage.setRGB(j, i, storedImage.getRGB(j, i))
                } else if (j > startIndex) {
                    compresedImage.setRGB(j - 1, i, storedImage.getRGB(j, i))
                }
                j++
            }
            startIndex += pathMatrix[i * storedImage.width + startIndex]
            i--
        }

        storedImage = compresedImage

        if (horizontal)
            transpose()
    }

    fun saveImage() {
        val bais: ByteArrayOutputStream = ByteArrayOutputStream()
        ImageIO.write(storedImage, "png", File(outputName))
        bais.close()
    }

    private fun transpose() {
        val transposedImage: BufferedImage = BufferedImage(storedImage.height, storedImage.width, storedImage.type)
        for (i in 0 until storedImage.width) {
            for (j in 0 until storedImage.height) {
                transposedImage.setRGB(j, i, storedImage.getRGB(i, j))
            }
        }
        storedImage = transposedImage
    }

    private fun calculateEnergies() {
        for (i in 0 until storedImage.height) {
            for (j in 0 until storedImage.width) {
                calculateEnergy(i, j)
            }
        }
    }

    private fun calculateEnergy(y: Int, x: Int) {
        val xNeighbors = getXNeighbors(x)
        val colorXLeft = Color(storedImage.getRGB(xNeighbors.first, y))
        val colorXRight = Color(storedImage.getRGB(xNeighbors.second, y))

        val yNeighbors = getYNeighbors(y)
        val colorYLeft = Color(storedImage.getRGB(x, yNeighbors.first))
        val colorYRight = Color(storedImage.getRGB(x, yNeighbors.second))

        val energy = sqrt(calculateSquaredGradient(colorXLeft, colorXRight)
                + calculateSquaredGradient(colorYLeft, colorYRight))

        energies.add(energy)
        maxEnergy = if (energy > maxEnergy) energy else maxEnergy
    }

    private fun calculateSquaredGradient(colorLeft: Color, colorRight: Color): Double {
        val R = (colorLeft.red - colorRight.red).toDouble()
        val G = (colorLeft.green - colorRight.green).toDouble()
        val B = (colorLeft.blue - colorRight.blue).toDouble()

        return R.pow(2) + G.pow(2) + B.pow(2)
    }

    private fun getXNeighbors(x: Int): Pair<Int, Int> {
        return when {
            x == 0                      -> Pair(0, 2)
            x == storedImage.width - 1  -> Pair(storedImage.width - 3, storedImage.width - 1)
            else                        -> Pair(x - 1, x + 1)
        }
    }

    private fun getYNeighbors(y: Int): Pair<Int, Int> {
        return when {
            y == 0                      -> Pair(0, 2)
            y == storedImage.height - 1 -> Pair(storedImage.height - 3, storedImage.height - 1)
            else                        -> Pair(y - 1, y + 1)
        }
    }

}