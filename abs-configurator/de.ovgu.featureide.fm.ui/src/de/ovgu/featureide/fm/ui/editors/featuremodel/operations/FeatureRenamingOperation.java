/* FeatureIDE - An IDE to support feature-oriented software development
 * Copyright (C) 2005-2012  FeatureIDE team, University of Magdeburg
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 *
 * See http://www.fosd.de/featureide/ for further information.
 */
package de.ovgu.featureide.fm.ui.editors.featuremodel.operations;

import de.ovgu.featureide.fm.core.FeatureModel;

/**
 * Operation with functionality to rename features. Provides undo/redo
 * functionality.
 * 
 * @author Fabian Benduhn
 */
public class FeatureRenamingOperation extends AbstractFeatureModelOperation {

	private static final String LABEL = "Rename Feature";
	private String oldName;
	private String newName;

	public FeatureRenamingOperation(FeatureModel featureModel, String oldName,
			String newName) {
		super(featureModel, LABEL);
		this.oldName = oldName;
		this.newName = newName;
	}

	@Override
	protected void redo() {
		featureModel.renameFeature(oldName, newName);
	}

	@Override
	protected void undo() {
		featureModel.renameFeature(newName, oldName);
	}

}