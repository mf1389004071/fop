/*-- $Id$ -- 

 ============================================================================
                   The Apache Software License, Version 1.1
 ============================================================================
 
    Copyright (C) 1999 The Apache Software Foundation. All rights reserved.
 
 Redistribution and use in source and binary forms, with or without modifica-
 tion, are permitted provided that the following conditions are met:
 
 1. Redistributions of  source code must  retain the above copyright  notice,
    this list of conditions and the following disclaimer.
 
 2. Redistributions in binary form must reproduce the above copyright notice,
    this list of conditions and the following disclaimer in the documentation
    and/or other materials provided with the distribution.
 
 3. The end-user documentation included with the redistribution, if any, must
    include  the following  acknowledgment:  "This product includes  software
    developed  by the  Apache Software Foundation  (http://www.apache.org/)."
    Alternately, this  acknowledgment may  appear in the software itself,  if
    and wherever such third-party acknowledgments normally appear.
 
 4. The names "Fop" and  "Apache Software Foundation"  must not be used to
    endorse  or promote  products derived  from this  software without  prior
    written permission. For written permission, please contact
    apache@apache.org.
 
 5. Products  derived from this software may not  be called "Apache", nor may
    "Apache" appear  in their name,  without prior written permission  of the
    Apache Software Foundation.
 
 THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED WARRANTIES,
 INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 FITNESS  FOR A PARTICULAR  PURPOSE ARE  DISCLAIMED.  IN NO  EVENT SHALL  THE
 APACHE SOFTWARE  FOUNDATION  OR ITS CONTRIBUTORS  BE LIABLE FOR  ANY DIRECT,
 INDIRECT, INCIDENTAL, SPECIAL,  EXEMPLARY, OR CONSEQUENTIAL  DAMAGES (INCLU-
 DING, BUT NOT LIMITED TO, PROCUREMENT  OF SUBSTITUTE GOODS OR SERVICES; LOSS
 OF USE, DATA, OR  PROFITS; OR BUSINESS  INTERRUPTION)  HOWEVER CAUSED AND ON
 ANY  THEORY OF LIABILITY,  WHETHER  IN CONTRACT,  STRICT LIABILITY,  OR TORT
 (INCLUDING  NEGLIGENCE OR  OTHERWISE) ARISING IN  ANY WAY OUT OF THE  USE OF
 THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 
 This software  consists of voluntary contributions made  by many individuals
 on  behalf of the Apache Software  Foundation and was  originally created by
 James Tauber <jtauber@jtauber.com>. For more  information on the Apache 
 Software Foundation, please see <http://www.apache.org/>.
 
 */
package org.apache.fop.pdf;

// Java
import java.io.IOException;
import java.io.PrintWriter;

/**
 * generic PDF object.
 * 
 * A PDF Document is essentially a collection of these objects. A PDF
 * Object has a number and a generation (although the generation will always
 * be 0 in new documents).
 */
public abstract class PDFObject {

    /** the object's number */
    protected int number;

    /** the object's generation (0 in new documents) */
    protected int generation = 0;

    /**
     * create an empty object
     *
     * @param number the object's number
     */
    public PDFObject(int number) {
	this.number = number;
    }
	public PDFObject() {
		//do nothing
	}
	/**
	 * @return the PDF Object number
	 */
	public int getNumber() {
		return this.number;
	}
    /**
     * write the PDF represention of this object
     *
     * @param writer the PrintWriter to write the PDF to
     * @return the number of characters written
     */
    protected int output(PrintWriter writer) throws IOException {
	String pdf = this.toPDF();
	writer.write(pdf);
	return pdf.length();
    }

    /**
     * the PDF representation of a reference to this object
     *
     * @return the reference string
     */
    public String referencePDF() {
	String p = this.number + " " + this.generation + " R";
	return p;
    }

    /**
     * represent object as PDF
     *
     * @return PDF string
     */
    abstract String toPDF();
}
