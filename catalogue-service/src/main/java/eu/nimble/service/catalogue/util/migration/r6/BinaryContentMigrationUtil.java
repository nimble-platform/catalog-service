package eu.nimble.service.catalogue.util.migration.r6;

import eu.nimble.service.model.ubl.commonbasiccomponents.BinaryObjectType;
import eu.nimble.utility.persistence.JPARepositoryFactory;
import eu.nimble.utility.persistence.binary.BinaryContentService;
import eu.nimble.utility.persistence.binary.ImageScaler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * Created by suat on 05-Dec-18.
 */
@Component
public class BinaryContentMigrationUtil {

    private static final Logger logger = LoggerFactory.getLogger(BinaryContentMigrationUtil.class);

    @Autowired
    private BinaryContentService binaryContentService;

    @Autowired
    private JPARepositoryFactory repoFactory;

    @Autowired
    private ImageScaler imageScaler;

    public void migrateBinaryObjects() {

        List<BinaryObjectType> binaryObjects = repoFactory.forCatalogueRepository(true).getEntities(BinaryObjectType.class);
        logger.info("Total binary documents: {}", binaryObjects.size());

        for(int i=0; i<binaryObjects.size(); i++) {
            BinaryObjectType binaryObject = binaryObjects.get(i);
            if(binaryObject.getValue() == null) {
                logger.info("No binary content for binary object: {}, hjid: {}", i, binaryObject.getHjid());
                continue;
            }

            InputStream in = new ByteArrayInputStream(binaryObject.getValue());

            // try to parse the inputstream to an image
            BufferedImage image = null;
            try {
                image = ImageIO.read(in);
            } catch (IOException e) {
                logger.warn("Failed to read input stream to buffered image.", e);
                image = null;
            }

            try {
                in.close();
            } catch (IOException e) {
                logger.warn("Failed to close inputstream", e);
                image = null;
            }


            byte[] originalContentBytes;
            if(image != null) {
                try {
                    in = new ByteArrayInputStream(binaryObject.getValue());
                    BufferedImage thumbnail = imageScaler.scale(in, true);
                    in.close();
                    in = new ByteArrayInputStream(binaryObject.getValue());
                    BufferedImage original = imageScaler.scale(in, false);
                    in.close();

                    String formatName = "png";
                    if(binaryObject.getMimeCode().contains("jpeg")) {
                        formatName = "jpg";
                    } else if(binaryObject.getMimeCode().contains("gif")) {
                        formatName = "gif";
                    }

                    ByteArrayOutputStream thumbnailBytes = new ByteArrayOutputStream();
                    ImageIO.write(thumbnail, formatName, thumbnailBytes);
                    binaryObject.setValue(thumbnailBytes.toByteArray());
                    thumbnailBytes.flush();
                    thumbnailBytes.close();

                    // original binary content data
                    ByteArrayOutputStream originalImageBytes = new ByteArrayOutputStream();
                    ImageIO.write(original, formatName, originalImageBytes);
                    originalContentBytes = originalImageBytes.toByteArray();
                    originalImageBytes.flush();
                    originalImageBytes.close();


                } catch (IOException e) {
                    logger.error("Failed to scale image with uri: {}, hjid: {}", binaryObject.getUri(), binaryObject.getHjid());
                    continue;
                }

            } else {
                originalContentBytes = binaryObject.getValue();
                binaryObject.setValue(null);
            }

            // store the original object in the binarycontentdb
            BinaryObjectType originalBinaryObject = new BinaryObjectType();
            originalBinaryObject.setValue(originalContentBytes);
            originalBinaryObject.setMimeCode(binaryObject.getMimeCode());
            originalBinaryObject.setFileName(binaryObject.getFileName());
            originalBinaryObject = binaryContentService.createContent(originalBinaryObject);

            // refer to the original content from the initial binary object
            binaryObject.setUri(originalBinaryObject.getUri());
            binaryObject = repoFactory.forCatalogueRepository().updateEntity(binaryObject);

            logger.info("Processed binary content: {}, hjid: {}", i, binaryObject.getHjid());
        }
    }

    private void closeResources(Connection c, PreparedStatement ps, ResultSet rs, String msg) {
        if (c != null) {
            try {
                if (!c.isClosed()) {
                    c.close();
                }
            } catch (SQLException e) {
                logger.warn("Failed to close connection: {}", msg, e);
            }
        }
        if (ps != null) {
            try {
                if (!ps.isClosed()) {
                    ps.close();
                }
            } catch (SQLException e) {
                logger.warn("Failed to close prepared statement: {}", msg, e);
            }
        }
        if (rs != null) {
            try {
                if (!rs.isClosed()) {
                    rs.close();
                }
            } catch (SQLException e) {
                logger.warn("Failed to close result set: {}", msg, e);
            }
        }
    }
}
