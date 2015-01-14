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
package name.vdg.respi55;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.widget.TextView;

public class TextActivity extends ActionBarActivity {
	public final static String EXTRA_TITLE_ID = "name.vdg.respi55.TEXT_TITLE_ID";
	public final static String EXTRA_RAW_HTML_ID = "name.vdg.respi55.TEXT_RAW_HTML_ID";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	    // Get the message from the intent
	    Intent intent = getIntent();
	    int titleId = intent.getIntExtra(EXTRA_TITLE_ID, R.string.app_name);
	    int htmlId = intent.getIntExtra(EXTRA_RAW_HTML_ID, R.string.app_name);

		setContentView(R.layout.activity_text);
		getSupportActionBar().setTitle(titleId);
		
		final TextView textView = (TextView) findViewById(R.id.textView1);
		
		Resources res = getResources();
		
		InputStream inputStream = res.openRawResource(htmlId);
		BufferedReader r = new BufferedReader(new InputStreamReader(inputStream));
		StringBuilder total;
		try {
			total = new StringBuilder(inputStream.available());
			String line;
			while ((line = r.readLine()) != null) {
				total.append(line);
				total.append('\n');
			}
			textView.setText(Html.fromHtml(total.toString()));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		textView.setMovementMethod(LinkMovementMethod.getInstance());

	}

}
