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

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import android.content.Intent;
import android.content.Context;

import android.net.Uri;

import androidx.recyclerview.widget.RecyclerView;

import com.yakovlevegor.DroidRec.R;

public class ContributorsAdapter extends RecyclerView.Adapter<ContributorsAdapter.ViewHolder> {

    private Context mainContext;

    private String[] contributorsNames;

    private String[] contributorsRoles;

    private String[] contributorsLinks;

    public class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView nameText;
        private final TextView roleText;

        public ViewHolder(View v) {
            super(v);
            v.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String openUrl = contributorsLinks[getAdapterPosition()];
                    if (openUrl.isEmpty() == false) {
                        Intent openPage = new Intent(Intent.ACTION_VIEW);
                        openPage.setData(Uri.parse(openUrl));
                        openPage.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        mainContext.startActivity(openPage);
                    }
                }
            });
            nameText = (TextView) v.findViewById(R.id.contributor_name);
            roleText = (TextView) v.findViewById(R.id.contributor_role);
        }

        public TextView getNameText() {
            return nameText;
        }

        public TextView getRoleText() {
            return roleText;
        }
    }

    public ContributorsAdapter(Context context) {
        mainContext = context;
        contributorsNames = context.getResources().getStringArray(R.array.contributors_names);
        contributorsRoles = context.getResources().getStringArray(R.array.contributors_roles);
        contributorsLinks = context.getResources().getStringArray(R.array.contributors_profiles);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        View v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.contributors_layout, viewGroup, false);

        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, final int position) {
        viewHolder.getNameText().setText(contributorsNames[position]);
        viewHolder.getRoleText().setText(contributorsRoles[position]);
    }

    @Override
    public int getItemCount() {
        return contributorsNames.length;
    }
}
