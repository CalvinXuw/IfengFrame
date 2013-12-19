/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ifeng.util.database;

import android.content.ContentResolver;
import android.database.sqlite.SQLiteDatabase;

import com.ifeng.BaseApplicaion;
import com.ifeng.util.logging.Log;

/**
 * Abstract helper base class for ContentResolver transactions.
 */
public abstract class SQLiteTransaction {

	/** log tag. */
	private final String TAG = getClass().getSimpleName();

	/** if enabled, logcat will output the log. */
	private static final boolean DEBUG = BaseApplicaion.DEBUG;

	/**
	 * Executes the statements that form the transaction.
	 * 
	 * @param cr
	 *            A ContentResolver.
	 * @return {@code true} if the transaction should be committed.
	 */
	protected abstract boolean performTransaction(ContentResolver cr);

	/**
	 * Runs the transaction against the database. The results are committed if
	 * {@link #performTransaction(SQLiteDatabase)} completes normally and
	 * returns {@code true}.
	 * 
	 * @param db
	 *            DataBase
	 */
	public void run(ContentResolver cr) {
		try {
			performTransaction(cr);
		} catch (Exception e) {
			if (DEBUG) {
				Log.e(TAG, e);
			}
		}
	}
}
