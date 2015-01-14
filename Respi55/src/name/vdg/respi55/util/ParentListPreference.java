/* Respi 5/5
 *
 * Copyright 2014-2015 Julien Viard de Galbert
 *
 * This file is part of Respi 5/5.
 *
 * Respi 5/5 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * Respi 5/5 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Respi 5/5.  If not, see <http://www.gnu.org/licenses/>.
 */
package name.vdg.respi55.util;

import android.content.Context;
import android.preference.ListPreference;
import android.util.AttributeSet;

public class ParentListPreference extends ListPreference {

	public ParentListPreference(Context context) {
		super(context);
		// TODO Auto-generated constructor stub
	}

	public ParentListPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
		// TODO Auto-generated constructor stub
	}

	@Override
	public void setValue(String value) {
	    String mOldValue = getValue();
	    super.setValue(value);
	    if (!value.equals(mOldValue)) {
	        notifyDependencyChange(shouldDisableDependents());
	    }
	}

	@Override
	public boolean shouldDisableDependents() {
	    boolean shouldDisableDependents = super.shouldDisableDependents();
	    String value = getValue();
	    boolean valueDisable;
		// enable/disable dependency based on numerical values
		try {
			valueDisable = (Integer.parseInt(value) <= 0);
		} catch (NumberFormatException e) {
		    // if not a number, enable to be sure
			valueDisable = false;
		}
	    return shouldDisableDependents || value == null || valueDisable;
	}
}
