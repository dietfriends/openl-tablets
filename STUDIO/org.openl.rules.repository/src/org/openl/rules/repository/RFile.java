package org.openl.rules.repository;

import org.openl.rules.repository.exceptions.RModifyException;
import org.openl.rules.repository.exceptions.RRepositoryException;

import java.io.InputStream;

/**
 * OpenL Rules File.
 * It stores content of physical files.
 *
 * @author Aleh Bykhavets
 *
 */
public interface RFile extends REntity {
    /**
     * Gets mime type of the file.
     *
     * @return mime type
     */
    public String getMimeType();

    /**
     * Returns size of the file's content in bytes.
     *
     * @return size of content or <code>-1</code> if cannot determine it.
     */
    public long getSize();

    /**
     * Gets content of the file.
     * It is highly apreciated to close stream right after it is no longer needed.
     *
     * @return content stream with content of file
     * @throws RRepositoryException if failed
     */
    public InputStream getContent() throws RRepositoryException;

    /**
     * Sets/Updates content of the file.
     * At the end input stream will be closed.
     *
     * @param inputStream stream with new content of the file
     * @throws RModifyException if failed
     */
    public void setContent(InputStream inputStream) throws RModifyException;
}
