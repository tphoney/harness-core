package software.wings.beans.yaml;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class FullSyncChangeset {
  List<GitFileChange> gitFileChanges;
  List<FullSyncError> yamlErrors;
}
