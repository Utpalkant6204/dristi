import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:auto_route/auto_route.dart';
import 'package:pucardpg/blocs/app-init-bloc/app_init.dart';
import 'package:pucardpg/blocs/auth-bloc/authbloc.dart';
import 'package:pucardpg/blocs/localization-bloc/localization.dart';
import 'package:pucardpg/blocs/project-bloc/project.dart';
import 'package:pucardpg/blocs/user-bloc/userbloc.dart';
import 'package:pucardpg/widget/localized.dart';

@RoutePage()
class HomeScreen extends LocalizedStatefulWidget {
  const HomeScreen({
    super.key,
    super.appLocalizations,
  });

  @override
  State<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends LocalizedState<HomeScreen> {
  @override
  Widget build(BuildContext context) {
    return PopScope(
        canPop: true,
        child: BlocBuilder<AppInitialization, InitState>(
            builder: (context, state) {
          final actionMap = state.entityActionMapping;
          return BlocBuilder<AuthBloc, AuthState>(
              builder: (context, state) => state.maybeWhen(
                  orElse: () => const Offstage(),
                  authenticated: (accesstoken, refreshtoken, userRequest) =>
                      BlocBuilder<LocalizationBloc, LocalizationState>(
                          builder: (context, state) {
                        return BlocBuilder<ProjectBloc, ProjectState>(
                            builder: (context, projectState) {
                          if (projectState is ProjectSelectedState) {
                            return BlocBuilder<UserBloc, UserState>(
                                builder: (context, currentUserState) {
                              context.read<UserBloc>().add(UserEvent.searchUser(
                                  uuid: userRequest!.uuid,
                                  actionMap: actionMap));
                              if (currentUserState is UserUserState) {
                                return const Column(
                                  mainAxisAlignment: MainAxisAlignment.start,
                                  crossAxisAlignment: CrossAxisAlignment.start,
                                  children: [],
                                );
                              } else {
                                return const Center(
                                    child: Text('No Projects Found'));
                              }
                            });
                          } else {
                            return const Center(child: Text('No Users Found'));
                          }
                        });
                      })));
        }));
  }
}
