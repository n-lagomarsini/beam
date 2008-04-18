/*
 * $Id: ShowStatusBarAction.java,v 1.1 2006/11/16 09:14:28 marcop Exp $
 *
 * Copyright (C) 2002 by Brockmann Consult (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.esa.beam.visat.actions;

import org.esa.beam.framework.ui.command.CommandEvent;
import org.esa.beam.framework.ui.command.ExecCommand;
import org.esa.beam.visat.VisatApp;

/**
 * This Action toggels the visibility of the status bar.
 *
 * @author Marco Peters
 * @version $Revision: 1.1 $ $Date: 2006/11/16 09:14:28 $
 */
public class ShowStatusBarAction extends ExecCommand {


    @Override
    public void actionPerformed(final CommandEvent event) {
        VisatApp.getApp().setStatusBarVisible(isSelected());
    }
}
