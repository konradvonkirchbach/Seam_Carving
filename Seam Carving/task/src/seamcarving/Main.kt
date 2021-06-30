package seamcarving

import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import javax.imageio.ImageIO

fun saveImage(image: BufferedImage, name: String): Unit {
    val bais: ByteArrayOutputStream = ByteArrayOutputStream()
    ImageIO.write(image, "png", File(name))
    bais.close()
}

fun readInImage(name: String): BufferedImage {
    val file = File(name)
    if (!file.extension.endsWith("png")) {
        throw RuntimeException("Not png format")
    }
    return ImageIO.read(File(name))
}

fun drawImage(width: Int, height: Int): BufferedImage {
    var bufferedImage: BufferedImage = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)

    // make all black
    for (i in 0 until width) {
        for (k in 0 until height) {
            bufferedImage.setRGB(i, k, 0x000000)
        }
    }

    // make edges red
    if (width > height) {
        val offset = width / height
        for (h in 0 until height) {
            for (unit in 0 until offset) {
                bufferedImage.setRGB(offset * h + unit, h, 0xFF0000)
                bufferedImage.setRGB(width - 1 - offset*h - unit, h, 0xFF0000)
            }
        }
    } else {
        val offset = height / width
        for (w in 0 until width) {
            for (unit in 0 until offset) {
                bufferedImage.setRGB(w, offset * w + unit, 0xFF0000)
                bufferedImage.setRGB(w, width - 1 - offset*w - unit, 0xFF0000)
            }
        }
    }

    return bufferedImage
}

fun invertImage(image: BufferedImage): Unit {
    val height = image.height
    val width = image.width

    for (h in 0 until height) {
        for (w in 0 until width) {
            image.setRGB(w, h, 0xFFFFFF - image.getRGB(w, h))
        }
    }
}

fun getParamOfFlag(args: Array<String>, flag: String): String = args[args.indexOf(flag) + 1]

fun main(args: Array<String>) {
    val inName = getParamOfFlag(args, "-in")
    val outName = getParamOfFlag(args, "-out")
    val width = getParamOfFlag(args, "-width").toInt()
    val height = getParamOfFlag(args, "-height").toInt()

    val imageHandler = ImageHandler(inName, outName)
    imageHandler.compress(width, height)
    imageHandler.saveImage()
}

fun main() {
    println("Enter rectangle width:")
    val width: Int = readLine()!!.toInt()

    println("Enter rectangle height:")
    val height: Int = readLine()!!.toInt()

    println("Enter output image name:")
    val imageName: String = readLine()!!

    if (!imageName.endsWith(".png"))
        return

    val image = drawImage(width, height)
    saveImage(image, imageName)
}
