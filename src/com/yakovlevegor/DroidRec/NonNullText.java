/*
 * This is free and unencumbered software released into the public domain.
 *
 * Anyone is free to copy, modify, publish, use, compile, sell, or
 * distribute this software, either in source code form or as a compiled
 * binary, for any purpose, commercial or non-commercial, and by any
 * means.
 *
 * In jurisdictions that recognize copyright laws, the author or authors
 * of this software dedicate any and all copyright interest in the
 * software to the public domain. We make this dedication for the benefit
 * of the public at large and to the detriment of our heirs and
 * successors. We intend this dedication to be an overt act of
 * relinquishment in perpetuity of all present and future rights to this
 * software under copyright law.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS BE LIABLE FOR ANY CLAIM, DAMAGES OR
 * OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 *
 * For more information, please refer to <http://unlicense.org/>
*/

package com.yakovlevegor.DroidRec;

import android.preference.EditTextPreference;
import android.widget.TextView;
import android.widget.EditText;
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.text.Editable;
import android.text.TextWatcher;

import com.yakovlevegor.DroidRec.R;

public class NonNullText extends EditTextPreference {

    private EditText textEdit;

    private String inputData;

    private String persistedString;

    private String defaultString;

    private String prefName;

    public NonNullText(Context context, AttributeSet attrs) {
        super(context, attrs);
        prefName = getKey();
    }

    private class InputValidator implements TextWatcher {

        public void afterTextChanged(Editable s) {}

        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

        public void onTextChanged(CharSequence s, int start, int before, int count) {

            String newInputData = s.toString();

            if (newInputData.length() < 10) {
                if (newInputData.contentEquals("") == false) {
                    int parsedValue = Integer.parseInt(newInputData);
                    if (prefName.contentEquals("bitratevalue") == true && parsedValue > 250000000) {
                        textEdit.setText(inputData);
                    } else if (prefName.contentEquals("fpsvalue") == true && parsedValue > 300) {
                        textEdit.setText(inputData);
                    } else {
                        inputData = newInputData;
                    }
                }
            } else {
                textEdit.setText(inputData);
            }

        }
    }

    @Override
    public void onSetInitialValue(boolean restoreValue, Object defaultValue) {
        super.onSetInitialValue(restoreValue, defaultValue);
        if (defaultValue != null) {
            String valueString = (String) defaultValue;
            defaultString = valueString;
        }
    }


    @Override
    public void onAddEditTextToDialogView(View dialogView, EditText editText) {
        super.onAddEditTextToDialogView(dialogView, editText);
        textEdit = editText;

        persistedString = NonNullText.this.getPersistedString(defaultString);
        editText.addTextChangedListener(new InputValidator());

        if (persistedString.startsWith("0") || persistedString.contentEquals("")) {
             persistString(defaultString);
             persistedString = defaultString;
        }

        inputData = persistedString;
    }

    @Override
    public void onDialogClosed(boolean positiveResult) {
        if (inputData.startsWith("0") || inputData.contentEquals("") || inputData.length() >= 10 || (prefName.contentEquals("bitratevalue") == true && (Integer.parseInt(inputData) < 128000))) {
            textEdit.setText(persistedString);
        } else {
            super.onDialogClosed(positiveResult);
        }
    }

}
