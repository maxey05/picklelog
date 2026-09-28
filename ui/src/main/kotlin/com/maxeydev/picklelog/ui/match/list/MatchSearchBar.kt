package com.maxeydev.picklelog.ui.match.list

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import com.maxeydev.picklelog.ui.R

@Composable
fun MatchSearchBar(
    text: String,
    onTextChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    OutlinedTextField(
        value = text,
        onValueChange = onTextChanged,
        label = { Text(stringResource(R.string.search_label)) },
        placeholder = { Text(stringResource(R.string.search_placeholder)) },
        leadingIcon = { Icon(painter = painterResource(R.drawable.ic_search), contentDescription = null) },
        trailingIcon =
            if (text.isEmpty()) {
                null
            } else {
                {
                    IconButton(
                        onClick = { onTextChanged("") },
                        modifier = Modifier.testTag(MatchListTestTags.SEARCH_CLEAR),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_close),
                            contentDescription = stringResource(R.string.search_clear),
                        )
                    }
                }
            },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
        modifier = modifier.fillMaxWidth().testTag(MatchListTestTags.SEARCH_FIELD),
    )
}
